/* Process Class
 * Giovanna Diaz, Alice Yang
 * 
 * Creates the Process object that is sent from TestMemoryManagement.java to memoryManagement.java
 *
 */
class Process {
	private String action;
	private int pid;
	private int textSegment;
	private int dataSegment;
	private int heapSegment;

	public Process(String action) { 
		this.action = action;
	}

	public Process(String action, int pid) { 
		this.action = action;
		this.pid = pid;
	}

	public Process(String action, int pid, int textSegment, int dataSegment, int heapSegment) { 
		this.action = action;
		this.pid = pid;
		this.textSegment = textSegment;
		this.dataSegment = dataSegment;
		this.heapSegment = heapSegment;
	}

	public String getAction() { return action; }
	public int getPid() { return pid; }
	public int getText() { return textSegment; }
	public int getData() { return dataSegment; }
	public int getHeap() { return heapSegment; }

} // EOAction