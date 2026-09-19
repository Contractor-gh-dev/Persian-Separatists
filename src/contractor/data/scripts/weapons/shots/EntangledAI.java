package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class EntangledAI implements MissileAIPlugin {
	private final MissileAPI missile;
	private final String parentTarget;
	private ShipAPI target = null;

	public EntangledAI(MissileAPI missile, String target) {
		this.missile = missile;
		this.parentTarget = target;
	}

	public void advance(float amount) {
		if (Global.getCombatEngine().isPaused())
			return;
		if (missile.isFizzling()) {
			Global.getCombatEngine().removeEntity(missile);
			return;
		}
		if (missile.getSource() == null) return;

		CombatEngineAPI engine = Global.getCombatEngine();
		if (target == null)
			for (ShipAPI ship : engine.getShips())
				if (ship.getId().equals(parentTarget))
					target = ship;

		if (target != null) {
			Vector2f targetLoc = new Vector2f(target.getLocation());
			targetLoc = AIUtils.getBestInterceptPoint(missile.getLocation(), missile.getMaxSpeed(), targetLoc, target.getVelocity());
			if (targetLoc == null)
				targetLoc = target.getLocation();

			float targetAngle = VectorUtils.getAngle(missile.getLocation(), targetLoc);
			targetAngle = MathUtils.getShortestRotation(missile.getFacing(), targetAngle);

			if (targetAngle < 0) {
				missile.giveCommand(ShipCommand.TURN_RIGHT);
			} else {
				missile.giveCommand(ShipCommand.TURN_LEFT);
			}
			if (Math.abs(targetAngle) < 10f)
				missile.setAngularVelocity(missile.getAngularVelocity() * 0.8f);

			missile.giveCommand(ShipCommand.ACCELERATE);

			if (MathUtils.isWithinRange(missile.getLocation(), targetLoc, 22f))
				missile.setFizzleTime(0f);
		}
	}
}
