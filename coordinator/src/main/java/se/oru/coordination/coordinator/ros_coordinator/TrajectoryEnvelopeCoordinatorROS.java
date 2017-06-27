package se.oru.coordination.coordinator.ros_coordinator;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import org.metacsp.multi.spatioTemporal.paths.TrajectoryEnvelope;
import org.ros.node.ConnectedNode;

import se.oru.coordination.coordination_oru.AbstractTrajectoryEnvelopeTracker;
import se.oru.coordination.coordination_oru.Dependency;
import se.oru.coordination.coordination_oru.RobotReport;
import se.oru.coordination.coordination_oru.TrackingCallback;
import se.oru.coordination.coordination_oru.TrajectoryEnvelopeCoordinator;

public class TrajectoryEnvelopeCoordinatorROS extends TrajectoryEnvelopeCoordinator {

	protected ConnectedNode node = null;
	
	public TrajectoryEnvelopeCoordinatorROS(int CONTROL_PERIOD, double TEMPORAL_RESOLUTION, final ConnectedNode connectedNode) {
		super(CONTROL_PERIOD, TEMPORAL_RESOLUTION);
		this.node = connectedNode;		
	}
	
	public TrajectoryEnvelopeCoordinatorROS(final ConnectedNode connectedNode) {
		this(1000, 1000.0, connectedNode);
	}
	
	@Override
	public long getCurrentTimeInMillis() {
		return TimeUnit.NANOSECONDS.toMillis(node.getCurrentTime().totalNsecs());
	}

	@Override
	public AbstractTrajectoryEnvelopeTracker getNewTracker(TrajectoryEnvelope te, TrackingCallback cb) {
		TrajectoryEnvelopeTrackerROS tet = new TrajectoryEnvelopeTrackerROS(te, this.TEMPORAL_RESOLUTION, solver, trackingPeriodInMillis, cb, this.node) {		
			//What should happen when a robot reaches a new pose along the path
			//In this implementation, simply update the GUI
			@Override
			public void onPositionUpdate() {
				
				System.out.println("-------------------- CALLED ON POS UPDATE -------------------");
				//Update the position of the robot in the GUI
				RobotReport rr = getRobotReport();
				double x = rr.getPose().getX();
				double y = rr.getPose().getY();
				double theta = rr.getPose().getTheta();
				panel.addGeometry("R" + te.getRobotID(), TrajectoryEnvelope.getFootprint(te.getFootprint(), x, y, theta), false, true, false, "#FF0000");
				
				//Draw an arrow if there is a critical point
				RobotReport rrWaiting = getRobotReport();
				synchronized (currentDependencies) {
					for (Dependency dep : currentDependencies) {
						if (dep.getWaitingTracker().equals(this)) {
							if (dep.getDrivingTracker() != null) {
								RobotReport rrDriving = dep.getDrivingTracker().getRobotReport();
								String arrowIdentifier = "_"+dep.getWaitingRobotID()+"-"+dep.getDrivingRobotID();
								panel.addArrow(arrowIdentifier, rrWaiting.getPose(), rrDriving.getPose());
							}
							else {
								String arrowIdentifier = "_"+dep.getWaitingRobotID()+"-"+dep.getDrivingRobotID();
								panel.addArrow(arrowIdentifier, rrWaiting.getPose(), traj.getPose()[rrWaiting.getCriticalPoint()]);										
							}
						}
					}							
				}
	
				//Refresh the GUI
				panel.updatePanel();
			}
		};
		return tet;
	}
	
	@Override
	public Comparator<TrajectoryEnvelope> getOrdering() {
		// TODO Auto-generated method stub
		return null;
	}

}
