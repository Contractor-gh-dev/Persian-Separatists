package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class SepWingSweepAI implements ShipSystemAIScript {
	private ShipAPI ship, parent;
	private FighterWingAPI wing;
	private SepWingSweepStats script;

	@Override
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.wing = ship.getWing();
		this.parent = wing.getSourceShip();
		this.script = (SepWingSweepStats) system.getScript();
	}

	@Override
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (target == null) {
			if (script.getMode())
				command();
			return;
		}

		if (parent.isPullBackFighters() && script.getMode()) {
			command();
			return;
		}

		Vector2f shipLoc = ship.getLocation();
		Vector2f targetLoc = target.getLocation();

		float distanceSquared = MathUtils.getDistanceSquared(shipLoc, targetLoc);

		if (distanceSquared > 250000f) {
			if (script.getMode())
				command();
		} else {
			if (!script.getMode())
				command();
		}
	}

	private void command() {
		for (ShipAPI fighter : wing.getWingMembers()) {
			fighter.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
		}

	}
}
