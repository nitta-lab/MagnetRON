package org.ntlab.animations;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ntlab.deltaViewer.DeltaGraphAdapter;
import org.ntlab.deltaViewer.MagnetRONScheduledThreadPoolExecutor;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
/**
 * 
 * 
 * @author Nitta Lab.
 */
public abstract class MagnetRONAnimation {

	// Test code (will be deleted)
	private static final String TAG = MagnetRONAnimation.class.getSimpleName();
	
	protected DeltaGraphAdapter mxgraph;
	protected mxGraphComponent mxgraphComponent;

	protected ThreadPoolExecutor threadPoolExecutor;
	protected ScheduledFuture<?> scheduledFuture;
	/**
	 * Initial delays the start of an animation.
	 *
	 * Cannot be negative. Setting to a negative number will result in {@link IllegalArgumentException}.
	 *
	 * @defaultValue 0ms
	 */
	private long initialDelay;
	private static final long DEFAULT_INITIAL_DELAY = 0L;
	/**
	 * Delays the interval between repeating an animation.
	 *
	 * Cannot be negative. Setting to a negative number will result in {@link IllegalArgumentException}.
	 *
	 * @defaultValue 0ms
	 */
	private long delay;
	private static final long DEFAULT_DELAY = 0L;

	/**
	 * Defines the direction/speed at which the {@code MagnetRONAnimation} is expected to
	 * be played.
	 * <p>
	 * The absolute value of {@code rate} indicates the speed which the
	 * {@code Animation} is to be played, while the sign of {@code rate}
	 * indicates the direction. A positive value of {@code rate} indicates
	 * forward play, a negative value indicates backward play and {@code 0.0} to
	 * stop a running {@code MagnetRONAnimation}.
	 * <p>
	 * Rate {@code 1.0} is normal play, {@code 2.0} is 2 time normal,
	 * {@code -1.0} is backwards, etc...
	 *
	 * <p>
	 * Inverting the rate of a running {@code MagnetRONAnimation} will cause the
	 * {@code MagnetRONAnimation} to reverse direction in place and play back over the
	 * portion of the {@code MagnetRONAnimation} that has already elapsed.
	 *
	 * @defaultValue 1.0
	 */
	private double rate;
	private static final double DEFAULT_RATE = 1.0;

	/**
	 * Read-only variable to indicate current direction/speed at which the
	 * {@code MagnetRONAnimation} is being played.
	 * <p>
	 * {@code currentRate} is not necessary equal to {@code rate}.
	 * {@code currentRate} is set to {@code 0.0} when animation is paused or
	 * stopped. {@code currentRate} may also point to different direction during
	 * reverse cycles when {@code reverse} is {@code true}
	 *
	 * @defaultValue 0.0
	 */
	private double currentRate;
	private static final double DEFAULT_CURRENT_RATE = 0.0;

	/**
	 * Defines the number of cycles in this animation. The {@code totalCycleCount}
	 * may be {@code INDEFINITE} for animations that repeat indefinitely, but
	 * must otherwise be > 0.
	 * <p>
	 * It is not possible to change the {@code totalCycleCount} of a running
	 * {@code MagnetRONAnimation}. If the value of {@code totalCycleCount} is changed for a
	 * running {@code MagnetRONAnimation}, the animation has to be stopped and started again to pick
	 * up the new value.
	 *
	 * @defaultValue 1
	 *
	 */
	private int totalCycleCount;
	private static final int DEFAULT_TOTAL_CYCLE_COUNT = 1;
	/**
	 * The current number of cycles in this animation.
	 * 
	 * @defaultValu 0
	 */
	private int currentCycleCount = 0;

	/**
	 * Used to specify an animation that repeats indefinitely, until the
	 * {@code stop()} method is called.
	 */
	private static final int INDEFINITE = -1;

	/**
	 * The status of the {@code MagnetRONAnimation}.
	 *
	 * In {@code MagnetRONAnimation} can be in one of three states:
	 * {@link Status#STOPPED}, {@link Status#PAUSED} or {@link Status#RUNNING}.
	 */
	private Status currentStatus;
	private static final Status DEFAULT_STATUS = Status.STOPPED;

	/**
	 * The action to be executed at the conclusion of this {@code MagnetRONAnimation}.
	 */
	private ActionListener onFinished;

	/**
	 * Defines whether this
	 * {@code MagnetRONAnimation} reverses direction on alternating cycles. If
	 * {@code true}, the
	 * {@code MagnetRONAnimation} will proceed reverses on the cycle. 
	 * Otherwise, animation will loop such that each cycle proceeds forward from the start.
	 *
	 * It is not possible to change the {@code reverse} flag of a running
	 * {@code MagnetRONAnimation}. If the value of {@code reverse} is changed for a
	 * running {@code MagnetRONAnimation}, the animation has to be stopped and started again to pick
	 * up the new value.
	 *
	 * @defaultValue false
	 */
	private boolean reverse;
	private static final boolean DEFAULT_REVERSE = false;

	/**
	 * The object to animate.
	 */
	private mxICell sourceCell;

	/**
	 * The initial point of sourceCell.
	 */
	private Point2D sourceInitPoint;

	/**
	 * The initial size of sourceCell.
	 */
	private Dimension2D sourceInitDimension;

	/**
	 * The possible state for MagnetRONAnimation.
	 */
	protected static enum Status {
		/**
		 * The paused state.
		 */
		PAUSED,
		/**
		 * The running state.
		 */
		RUNNING,
		/**
		 * The stopped state.
		 */
		STOPPED
	}

	private static int animationCount = 0;
	
	/**
	 * The constructor of {@code MagnetRONAnimation}.
	 * 
	 * @param mxgraph: visualization model
	 * @param mxgraphComponent: visualization model group
	 */
	protected MagnetRONAnimation(DeltaGraphAdapter mxgraph, mxGraphComponent mxgraphComponent) {
		this.mxgraph = mxgraph;
		this.mxgraphComponent = mxgraphComponent;
	}

	protected void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
		this.threadPoolExecutor = threadPoolExecutor;
	}

	public void setInitialDelay(long initialDelay) {
		this.initialDelay = initialDelay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	protected void setRate(double rate) {
		this.rate = rate;
	}

	protected void setCurrentRate(double currentRate) {
		this.currentRate = currentRate;
	}

	public void setTotalCycleCount(int totalCycleCount) {
		this.totalCycleCount = totalCycleCount;
	}

	protected void setCurrentCycleCount(int currentCycleCount) {
		this.currentCycleCount = currentCycleCount;
	}

	protected void setCurrentStatus(Status currentStatus) {
		this.currentStatus = currentStatus;
	}

	public void setOnFinished(ActionListener onFinished) {
		this.onFinished = onFinished;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	protected void setSourceCell(mxICell sourceCell) {
		this.sourceCell = sourceCell;
	}

	protected void setSourceInitialPoint(Point2D sourceInitPoint) {
		this.sourceInitPoint = sourceInitPoint;
	}

	protected void setSourceInitialDimension(Dimension2D sourceInitDimension) {
		this.sourceInitDimension = sourceInitDimension;
	}

	protected void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
		this.scheduledFuture = scheduledFuture;
	}

	protected abstract void setDestination(double x, double y);

	protected abstract void setVelocity(double x, double y);

	protected ThreadPoolExecutor getThreadPoolExecutor() {
		return threadPoolExecutor;
	}

	public long getInitialDelay() {
		if (initialDelay == 0L) return DEFAULT_INITIAL_DELAY;
		return initialDelay;
	}

	public long getDelay() {
		if (delay == 0L) return DEFAULT_DELAY;
		return delay;
	}

	protected double getRate() {
		if (rate == 0.0) return DEFAULT_RATE;
		return rate;
	}

	protected double getCurrentRate() {
		if (currentRate == 0.0) return DEFAULT_CURRENT_RATE;
		return currentRate;
	}

	public int getTotalCycleCount() {
		if (totalCycleCount == 0) return DEFAULT_TOTAL_CYCLE_COUNT;
		return totalCycleCount;
	}

	protected int getCurrentCycleCount() {
		return currentCycleCount;
	}

	protected Status getCurrentStatus() {
		if (currentStatus == null) return DEFAULT_STATUS;
		return currentStatus;
	}

	public ActionListener getOnFinished() {
		return onFinished;
	}

	public boolean getReverse() {
		if (!reverse) return DEFAULT_REVERSE;
		return reverse;
	}

	protected mxICell getSourceCell() {
		return sourceCell;
	}

	protected Point2D getSourceInitialPoint() {
		return sourceInitPoint;
	}

	protected Dimension2D getSourceInitialDimension() {
		return sourceInitDimension;
	}

	protected ScheduledFuture<?> getScheduledFuture() {
		return scheduledFuture;
	}

	/**
	 * Set expand or reduction animation of edge to targetPoint.
	 * Must be call {@code MagnetRONAnimation#init(mxICell, mxPoint, ThreadPoolExecutor)} before calling {@code MagnetRONAnimation#play()}.
	 * 
	 * @param sourceCell: edge object
	 * @param destinationPoint
	 */
	public void init(mxICell sourceCell, double destinationX, double destinationY, ThreadPoolExecutor threadPoolExecutor) {
		setSourceCell(sourceCell);
		setDestination(destinationX, destinationY);
		setThreadPoolExecutor(threadPoolExecutor);
		setSourceInitialPoint(getSourceCell().getGeometry().getPoint());
		setSourceInitialDimension(
				new Dimension((int) getSourceCell().getGeometry().getWidth(), 
						(int) getSourceCell().getGeometry().getHeight()));
		setCurrentCycleCount(0);
	}

	public void updateCurrentCycle() {
		if (!getReverse()) { // Animation direction is forward.
			setCurrentCycleCount((int) (currentCycleCount + Math.signum(getTotalCycleCount())));
		} else {
			setCurrentCycleCount((int) (currentCycleCount - Math.signum(getTotalCycleCount())));
		}
	}

	public void interpolate(double cycleCount) {

	}

	public void playFrom() {

	}

	public void play() {
		switch (getCurrentStatus()) {
			case STOPPED:
				if (getThreadPoolExecutor() != null & getThreadPoolExecutor() instanceof ScheduledThreadPoolExecutor) {
					ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (ScheduledThreadPoolExecutor) getThreadPoolExecutor();
					setThreadPoolExecutor(scheduledThreadPoolExecutor);
					ScheduledFuture<?> scheduledFuture = scheduledThreadPoolExecutor.scheduleWithFixedDelay(new TimerTask() {
						@Override
						public void run() {
							if(Math.abs(getCurrentCycleCount()) < Math.abs(getTotalCycleCount())) {
								// Test code (will be deleted)
								System.out.println(TAG + ": Run task " + getSourceCell().getId() + " " + MagnetRONAnimation.this.getClass().getSimpleName() + "-" + getCurrentCycleCount() + ". ThreadId=" + Thread.currentThread().getId());
								updateCurrentCycle();
								jumpTo(getCurrentCycleCount());
							} else if(Math.abs(getCurrentCycleCount()) >= Math.abs(getTotalCycleCount())){
								animationCount = 0;
								onFinished();
							}
						}
					}, getInitialDelay(), getDelay(), TimeUnit.MILLISECONDS);
					setScheduledFuture(scheduledFuture);
					setCurrentStatus(Status.RUNNING);
					animationCount = 1;
				};
				break;
			case PAUSED:
				if (getThreadPoolExecutor() != null & getThreadPoolExecutor() instanceof MagnetRONScheduledThreadPoolExecutor) {
					MagnetRONScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (MagnetRONScheduledThreadPoolExecutor) getThreadPoolExecutor();
					scheduledThreadPoolExecutor.resume();
					setCurrentStatus(Status.RUNNING);
				}
				break;
			default:
				break;
		};
	}
	
	/**
	 * Sleep main thread and wait for {@link MagnetRONAnimation#play()} to finish running.
	 */
	public static void waitAnimationEnd() {
		while (animationCount > 0) {
			try {
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// Buffer for another waiting animation. 
		try {
			Thread.sleep(30L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Play animation in sync with main thread.
	 */
	public void syncPlay() {
		if (getCurrentStatus() == Status.STOPPED) {
			try {
				Thread.sleep(getInitialDelay());
				while (true) {
					while (getCurrentStatus() == Status.PAUSED) {
						Thread.sleep(1L);
					}
					if(Math.abs(getCurrentCycleCount()) < Math.abs(getTotalCycleCount())) {
						// Test code (will be deleted)
						System.out.println(TAG + ": Run task " + getSourceCell().getId() + " " + MagnetRONAnimation.this.getClass().getSimpleName() + "-" + getCurrentCycleCount() + ". ThreadId=" + Thread.currentThread().getId());
						updateCurrentCycle();
						jumpTo(getCurrentCycleCount());
					} else if(Math.abs(getCurrentCycleCount()) >= Math.abs(getTotalCycleCount())){
						onFinished();
						break;
					}
					Thread.sleep(getDelay());
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void playFormStart() {

	}

	public void stop() {
		if (getCurrentStatus() == Status.RUNNING) {
			getScheduledFuture().cancel(true);
		}
		setCurrentStatus(Status.STOPPED);
		setCurrentRate(0.0);
	}

	public void pause() {
		if (getCurrentStatus() == Status.RUNNING) {
			if (getThreadPoolExecutor() != null && getThreadPoolExecutor() instanceof MagnetRONScheduledThreadPoolExecutor) {
				MagnetRONScheduledThreadPoolExecutor scheduledThreadPoolExecutor = (MagnetRONScheduledThreadPoolExecutor) getThreadPoolExecutor();
				scheduledThreadPoolExecutor.pause();
				setCurrentStatus(Status.PAUSED);
			}
		}

	}

	private final void onFinished() {
		stop();
		final ActionListener listener = getOnFinished();
		if (listener != null) {
			try {
				listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
			} catch (Exception e) {
				Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
			}
		}
	}

	protected abstract void jumpTo(int currentCycleCount);
}
