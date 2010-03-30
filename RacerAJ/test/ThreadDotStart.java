/**
 * @author Eric Bodden
 */
public class ThreadDotStart {
	
	public static void main(String[] args) throws InterruptedException {
		Task t = new Task(42);
		Thread thread = new Thread(t);
		thread.start();
		thread.join();
		t.nonFinalVar = 0;
		t.nonFinalVar = 1;
	}
	
	static class Task implements Runnable {
		
		int nonFinalVar;
		
		public Task(int param) {
			nonFinalVar = param;
		}

		public void run() {
			int a = nonFinalVar;
		}
	}
	
}
