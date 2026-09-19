package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class EntanglementAI implements ShipSystemAIScript {
	private ShipAPI ship;
	private EntanglementStats script;

	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.script = (EntanglementStats) ship.getSystem().getScript();
	}

	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		if (Global.getCombatEngine().isPaused())
			return;
		if (!AIUtils.canUseSystemThisFrame(ship) && !ship.getSystem().isOn())
			return;

		if (ship.isDirectRetreat()) {
			deactivate();
			return;
		}

		ShipAPI generalTarget = AIUtils.getNearestEnemy(ship);
		if (generalTarget != null) {
			if (MathUtils.getDistanceSquared(ship.getLocation(), generalTarget.getLocation()) > 3000f * 3000f) {
				deactivate();
				return;
			}
		} else if (target == null) {
			deactivate();
			return;
		}

		if (ship.getFluxLevel() > 0.95f) {
			deactivate();
			return;
		}

		if (target != null && !ship.getSystem().isOn())
			if (MathUtils.getDistanceSquared(ship.getLocation(), target.getLocation()) < 1800f * 1800f) {
				activate();
				return;
			}

		String daughterID = script.getDaughter();
		ShipAPI daughter = null;

		if (daughterID.isEmpty())
			return;

		List<ShipAPI> searchShips = Global.getCombatEngine().getShips();

		for (ShipAPI s : searchShips)
			if (s.getId().equals(daughterID))
				daughter = s;

		if (daughter == null)
			return;

		float ourHull = ship.getHullLevel() * 0.5f;
		float daughterHull = daughter.getHullLevel();
		float daughterFlux = daughter.getFluxLevel() * 0.25f;
		float daughterOverload = (daughter.getFluxTracker().isOverloaded()) ? 0.5f : 0f;

		if ((daughterHull - (daughterFlux + daughterOverload)) < ourHull)
			deactivate();
	}

	private void activate() {
		if (!ship.getSystem().isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}

	private void deactivate() {
		if (ship.getSystem().isActive())
			ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
	}
}
