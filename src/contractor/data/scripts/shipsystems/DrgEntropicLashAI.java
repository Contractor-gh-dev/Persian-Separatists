package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import static contractor.data.scripts.shipsystems.DrgEntropicLash.BASE_RANGE;

public class DrgEntropicLashAI implements ShipSystemAIScript {
	private ShipAPI ship;

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (target == null || !AIUtils.canUseSystemThisFrame(ship))
			return;
		if (target.getCollisionClass().equals(CollisionClass.NONE))
			return;
		if (target.isFighter())
			return;

		float range = ship.getMutableStats().getSystemRangeBonus().computeEffective(BASE_RANGE) * 0.95f;
		if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < range * range)
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}
}
