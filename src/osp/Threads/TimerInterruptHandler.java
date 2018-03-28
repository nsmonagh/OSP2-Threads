package osp.Threads;

import osp.IFLModules.IflTimerInterruptHandler;

public class TimerInterruptHandler extends IflTimerInterruptHandler {
	public void do_handleInterrupt() {
		ThreadCB.dispatch();
	}
}