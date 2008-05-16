/*******************************************************************************
 * Copyright (c) 2007 Chase Technology Ltd - http://www.chasetechnology.co.uk
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Satchwell (Chase Technology Ltd) - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xsl.launching.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.wst.xsl.debugger.DebugConstants;
import org.eclipse.wst.xsl.internal.launching.LaunchingPlugin;
import org.eclipse.wst.xsl.internal.launching.XSLTLaunchConfigurationDelegate;
import org.eclipse.wst.xsl.launching.IProcessorInstall;
import org.eclipse.wst.xsl.launching.config.LaunchHelper;

public class XSLDebugTarget extends XSLDebugElement implements IXSLDebugTarget
{
	private final byte[] STACK_FRAMES_LOCK = new byte[0];
	private final byte[] VALUE_MAP_LOCK = new byte[0];
	private final byte[] WRITE_LOCK = new byte[0];

	private final int CONNECT_ATTEMPTS = 10;
	private final int CONNECT_WAIT = 1000;

	private final IProcess process;
	private final ILaunch launch;
	private XSLThread thread;
	private IThread[] threads = new IThread[0];
	private IStackFrame[] stackFramesCache = new IStackFrame[0];

	private EventDispatchJob eventDispatch;

	private final Map<Integer,XSLVariable> variableMapCache = new HashMap<Integer, XSLVariable>();
	private final Map<XSLVariable, XSLValue> valueMapCache = new HashMap<XSLVariable, XSLValue>();
	private String name;
	private boolean suspended;
	private boolean terminated;

	private Socket requestSocket;
	private Socket eventSocket;
	private BufferedReader requestReader;
	private BufferedReader eventReader;
	private PrintWriter requestWriter;
	private boolean stale;

	public XSLDebugTarget(ILaunch launch, IProcess process, LaunchHelper launchHelper) throws CoreException
	{
		super(null);
		this.launch = launch;
		this.process = process;
		this.requestSocket = attemptConnect(launchHelper.getRequestPort());
		this.eventSocket = attemptConnect(launchHelper.getEventPort());

		if (!process.isTerminated())
		{
			try
			{
				this.eventReader = new BufferedReader(new InputStreamReader(eventSocket.getInputStream()));
				this.requestWriter = new PrintWriter(requestSocket.getOutputStream());
				this.requestReader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
			}
			catch (IOException e)
			{
				abort(Messages.getString("XSLDebugTarget.0"), e); //$NON-NLS-1$
			}
			this.thread = new XSLThread(this);
			this.threads = new IThread[]{ thread };
			this.eventDispatch = new EventDispatchJob();
			this.eventDispatch.schedule();
	
			DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		}
	}

    private void abort(String message, Throwable e) throws DebugException
	{
		if (!getDebugTarget().isTerminated())
			getDebugTarget().getProcess().terminate();
		throw new DebugException(new Status(IStatus.ERROR, LaunchingPlugin.PLUGIN_ID, DebugPlugin.INTERNAL_ERROR, message, e));
	}
	
	private Socket attemptConnect(int port) throws CoreException
	{
		Socket socket = null;
		for(int i=0;i<CONNECT_ATTEMPTS;i++)
		{	
			// break out if process is terminated
			if (process.isTerminated())
				break;
			try
			{
				socket = new Socket(Messages.getString("XSLDebugTarget.1"),port); //$NON-NLS-1$
			}
			catch (ConnectException e)
			{}
			catch (IOException e)
			{}
			if (socket != null)
				break;
			try
			{
				Thread.sleep(CONNECT_WAIT);
			}
			catch (InterruptedException e)
			{}
		}
		if (socket == null && !process.isTerminated())
			throw new CoreException(new Status(Status.ERROR, LaunchingPlugin.PLUGIN_ID, Messages.getString("XSLDebugTarget.2")+port+Messages.getString("XSLDebugTarget.3")+CONNECT_ATTEMPTS+Messages.getString("XSLDebugTarget.4"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return socket;
	}

	public IProcess getProcess()
	{
		return process;
	}

	public IThread[] getThreads() throws DebugException
	{
		return threads;
	}

	public boolean hasThreads() throws DebugException
	{
		return threads != null && threads.length > 0;
	}

	public String getName() throws DebugException
	{
		if (name == null)
		{
			try
			{
				IProcessorInstall install = XSLTLaunchConfigurationDelegate.getProcessorInstall(getLaunch().getLaunchConfiguration(), ILaunchManager.DEBUG_MODE);
				String type = install.getProcessorType().getLabel();
				name = type + " [" + install.getName() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (CoreException e)
			{
				throw new DebugException(e.getStatus());
			}
		}
		return name;
	}

	public boolean supportsBreakpoint(IBreakpoint breakpoint)
	{
		if (breakpoint.getModelIdentifier().equals(IXSLConstants.ID_XSL_DEBUG_MODEL) && breakpoint instanceof ILineBreakpoint)
		{
//			try
//			{
//				ILineBreakpoint lb = (ILineBreakpoint) breakpoint;
//				IMarker marker = lb.getMarker();
//				for (Iterator<?> iter = launchHelper.getPipeline().getTransformDefs().iterator(); iter.hasNext();)
//				{
//					LaunchTransform lt = (LaunchTransform) iter.next();
//					if (marker.getResource().getLocation().equals(lt.getLocation()))
//						return true;
//				}
//			}
//			catch (CoreException e)
//			{
//				LaunchingPlugin.log(e);
//			}
			return true;
		}
		return false;
	}

	@Override
	public IDebugTarget getDebugTarget()
	{
		return this;
	}

	@Override
	public ILaunch getLaunch()
	{
		return launch;
	}

	public boolean canTerminate()
	{
		return getProcess().canTerminate();
	}

	public boolean isTerminated()
	{
		return terminated;
	}

	public void terminate() throws DebugException
	{
		synchronized (WRITE_LOCK)
		{
			getProcess().terminate();
		}
	}

	public boolean canResume()
	{
		return !isTerminated() && isSuspended();
	}

	public boolean canSuspend()
	{
		return !isTerminated() && !isSuspended();
	}

	public boolean isSuspended()
	{
		return suspended;
	}

	public void resume() throws DebugException
	{
		sendRequest(DebugConstants.REQUEST_RESUME);
	}

	private void resumed(int detail)
	{
		suspended = false;
		thread.fireResumeEvent(detail);
	}

	private void suspended(int detail)
	{
		suspended = true;
		thread.fireSuspendEvent(detail);
	}

	public void suspend() throws DebugException
	{
		sendRequest(DebugConstants.REQUEST_SUSPEND);
	}

	public void breakpointAdded(IBreakpoint breakpoint)
	{
		if (supportsBreakpoint(breakpoint))
		{
			try
			{
				ILineBreakpoint lb = (ILineBreakpoint) breakpoint;
				if (breakpoint.isEnabled())
				{
					try
					{
						IMarker marker = lb.getMarker();
						if (marker != null)
						{
							URL file = marker.getResource().getLocation().toFile().toURI().toURL();
							sendRequest(DebugConstants.REQUEST_ADD_BREAKPOINT + " " + file + " " + lb.getLineNumber()); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					catch (CoreException e)
					{
						LaunchingPlugin.log(e);
					}
					catch (MalformedURLException e)
					{
						LaunchingPlugin.log(e);
					}
				}
			}
			catch (CoreException e)
			{
			}
		}
	}

	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta)
	{
		if (supportsBreakpoint(breakpoint))
		{
			try
			{
				ILineBreakpoint lb = (ILineBreakpoint) breakpoint;
				IMarker marker = lb.getMarker();
				if (marker != null)
				{
					URL file = marker.getResource().getLocation().toFile().toURI().toURL();
					sendRequest(DebugConstants.REQUEST_REMOVE_BREAKPOINT + " " + file + " " + lb.getLineNumber()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			catch (CoreException e)
			{
				LaunchingPlugin.log(e);
			}
			catch (MalformedURLException e)
			{
				LaunchingPlugin.log(e);
			}
		}
	}

	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta)
	{
		if (supportsBreakpoint(breakpoint))
		{
			try
			{
				if (breakpoint.isEnabled())
				{
					breakpointAdded(breakpoint);
				}
				else
				{
					breakpointRemoved(breakpoint, null);
				}
			}
			catch (CoreException e)
			{
			}
		}
	}

	public boolean canDisconnect()
	{
		// TODO implement disconnect
		return false;
	}

	public void disconnect() throws DebugException
	{
		// TODO implement disconnect
	}

	public boolean isDisconnected()
	{
		// TODO implement disconnect
		return false;
	}

	public boolean supportsStorageRetrieval()
	{
		return false;
	}

	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException
	{
		return null;
	}

	private void ready()
	{
		fireCreationEvent();
		installDeferredBreakpoints();
		try
		{
			sendRequest(DebugConstants.REQUEST_START);
		}
		catch (DebugException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint
	 * manager.
	 */
	private void installDeferredBreakpoints()
	{
		IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IXSLConstants.ID_XSL_DEBUG_MODEL);
		for (IBreakpoint element : breakpoints)
		{
			breakpointAdded(element);
		}
	}

	private void terminated()
	{
		terminated = true;
		suspended = true;
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);

		threads = new IThread[0];

		fireTerminateEvent();
	}

	/**
	 * Returns the current stack frames in the target.
	 */
	public IStackFrame[] getStackFrames() throws DebugException
	{
		synchronized (STACK_FRAMES_LOCK)
		{
			if (stale)
			{
				stale = false;
				String framesData = sendRequest(DebugConstants.REQUEST_STACK);
				String[] frames = framesData.split("\\$\\$\\$"); //$NON-NLS-1$
				IStackFrame[] sf = new IStackFrame[frames.length];
				List<IStackFrame> currentFrames = Arrays.asList(stackFramesCache);
				for (int i = 0; i < frames.length; i++)
				{
					String data = frames[i];
					XSLStackFrame frame = new XSLStackFrame(thread, data, i);
					int index;
					if ((index = currentFrames.indexOf(frame)) != -1)
					{
						XSLStackFrame curr = (XSLStackFrame)currentFrames.get(index);
						curr.setLineNumber(frame.getLineNumber());
						curr.setVariables(frame.getVariables());
						frame = curr;
					}
					sf[frames.length - i - 1] = frame;
				}
				stackFramesCache = sf;
			}
			return stackFramesCache;
		}
	}
	
/*
	private void init(String data)
	{

		String[] strings = data.split("\\|");
		String fileName = strings[0];
		try
		{
			URL url = new URL(fileName);
			Path p = new Path(url.getFile());
			xslFileName = (new Path(fileName)).lastSegment();

			String idString = strings[1];
			id = Integer.parseInt(idString);
			String pc = strings[2];
			lineNumber = Integer.parseInt(pc);
			String safename = strings[3];

			int theIndex;
			while ((theIndex = safename.indexOf("%@_PIPE_@%")) != -1)
			{
				safename = safename.substring(0, theIndex) + "|" + safename.substring(theIndex + "%@_PIPE_@%".length(), safename.length());
			}

			name = p.lastSegment() + " " + safename;

			int numVars = strings.length - 4;
			variables = new IVariable[numVars];
			for (int i = 0; i < numVars; i++)
			{
				String val = strings[i + 4];
				int index = val.lastIndexOf('&');
				int slotNumber = Integer.parseInt(val.substring(index + 1));
				val = val.substring(0, index);
				index = val.lastIndexOf('&');
				String name = val.substring(0, index);
				String scope = val.substring(index + 1);
				variables[i] = new XSLVariable(this, scope, name, slotNumber);
			}
		}
		catch (MalformedURLException e)
		{
			LaunchingPlugin.log(e);
		}
	}
*/

	private void ressetStackFramesCache()
	{
		stale = true;
		synchronized (VALUE_MAP_LOCK)
		{
			valueMapCache.clear();
		}
	}

	/**
	 * Single step the interpreter.
	 */
	public void stepOver() throws DebugException
	{
		sendRequest(DebugConstants.REQUEST_STEP_OVER);
	}

	public void stepInto() throws DebugException
	{
		sendRequest(DebugConstants.REQUEST_STEP_INTO);
	}

	public void stepReturn() throws DebugException
	{
		sendRequest(DebugConstants.REQUEST_STEP_RETURN);
	}

	public XSLVariable getVariable(int varId) throws DebugException
	{
		synchronized (variableMapCache)
		{
			XSLVariable var = variableMapCache.get(varId);
			if (var == null)
			{
				var = new XSLVariable(this,varId);
				String res = sendRequest(DebugConstants.REQUEST_VARIABLE + " " + varId); //$NON-NLS-1$
				String[] data = res.split("&"); //$NON-NLS-1$
				var.setScope(data[0]);
				var.setName(data[1]);
				variableMapCache.put(varId,var);
			}
			return var;
		}
	}

	public IValue getVariableValue(XSLVariable variable) throws DebugException
	{
		synchronized (VALUE_MAP_LOCK)
		{
			XSLValue value = (XSLValue) valueMapCache.get(variable);
			if (value == null)
			{
				if (isSuspended())
				{
					String res = sendRequest(DebugConstants.REQUEST_VALUE + " " + variable.getId()); //$NON-NLS-1$
					String[] data = res.split("&"); //$NON-NLS-1$
					String type = data[0];
					String theval;
					if (data.length > 1)
						theval = data[1];
					else
						theval = ""; //$NON-NLS-1$
					value = new XSLValue(this, type, theval);
					valueMapCache.put(variable, value);
				}
				else
				{
					// anything as long as not null!
					value = new XSLValue(this, "G", ""); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return value;
		}
	}

	private String sendRequest(String request) throws DebugException
	{
		String response = null;
		synchronized (WRITE_LOCK)
		{
			// System.out.println("REQUEST: " + request);
			requestWriter.println(request);
			requestWriter.flush();
			try
			{
				// wait for response
				response = requestReader.readLine();
				// System.out.println("RESPONSE: " + response);
			}
			catch (IOException e)
			{
				abort(Messages.getString("XSLDebugTarget.19") + request, e); //$NON-NLS-1$
			}
		}
		return response;
	}

	private void breakpointHit(String event)
	{
		// determine which breakpoint was hit, and set the thread's breakpoint
		int lastSpace = event.lastIndexOf(' ');
		if (lastSpace > 0)
		{
			String line = event.substring(lastSpace + 1);
			int lineNumber = Integer.parseInt(line);
			IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IXSLConstants.ID_XSL_DEBUG_MODEL);
			for (IBreakpoint breakpoint : breakpoints)
			{
				if (supportsBreakpoint(breakpoint))
				{
					if (breakpoint instanceof ILineBreakpoint)
					{
						ILineBreakpoint lineBreakpoint = (ILineBreakpoint) breakpoint;
						try
						{
							if (lineBreakpoint.getLineNumber() == lineNumber)
							{
								thread.setBreakpoints(new IBreakpoint[]{ breakpoint });
								break;
							}
						}
						catch (CoreException e)
						{
						}
					}
				}
			}
		}
		suspended(DebugEvent.BREAKPOINT);
	}

	private class EventDispatchJob extends Job
	{

		public EventDispatchJob()
		{
			super(Messages.getString("XSLDebugTarget.20")); //$NON-NLS-1$
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor)
		{
			String event = ""; //$NON-NLS-1$
			while (!isTerminated() && event != null)
			{
				try
				{
					event = eventReader.readLine();
					if (event != null)
					{
						thread.setBreakpoints(null);
						thread.setStepping(false);
						if (event.equals("ready")) //$NON-NLS-1$
						{
							ready();
						}
						else if (event.equals("stopped")) //$NON-NLS-1$
						{
							try
							{
								terminate();
							}
							catch (DebugException e)
							{
							}
						}
						else if (event.equals("terminated")) //$NON-NLS-1$
						{
							terminated();
						}
						else if (event.startsWith("resumed")) //$NON-NLS-1$
						{
							if (event.endsWith("step")) //$NON-NLS-1$
							{
								thread.setStepping(true);
								resumed(DebugEvent.STEP_OVER);
							}
							else if (event.endsWith("client")) //$NON-NLS-1$
							{
								resumed(DebugEvent.CLIENT_REQUEST);
							}
							else
							{
								// System.out.println("Did not understand event:
								// " + event);
							}
						}
						else if (event.startsWith("suspended")) //$NON-NLS-1$
						{
							// clear down the frames so that they are re-fetched
							ressetStackFramesCache();
							if (event.endsWith("client")) //$NON-NLS-1$
							{
								suspended(DebugEvent.CLIENT_REQUEST);
							}
							else if (event.endsWith("step")) //$NON-NLS-1$
							{
								suspended(DebugEvent.STEP_END);
							}
							else if (event.indexOf("breakpoint") >= 0) //$NON-NLS-1$
							{
								breakpointHit(event);
							}
							else
							{
								// System.out.println("Did not understand event:
								// " + event);
							}
						}
						else
						{
							// System.out.println("Did not understand event: " +
							// event);
						}
					}
				}
				catch (IOException e)
				{
					terminated();
				}
			}
			return Status.OK_STATUS;
		}
	}
}