package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepAndrossPirateLaunch extends BaseShipSystemScript {
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		if (!ship.isAlive())
			return;

		int ammo = 0;

		for (WeaponAPI w : ship.getAllWeapons()) {
			if (!w.isDecorative())
				ammo += w.getAmmo();
		}

		if (ammo <= 2 || ship.getHullLevel() <= 0.2f) {
			CombatEngineAPI engine = Global.getCombatEngine();
			Vector2f location = new Vector2f(ship.getLocation());
			Vector2f target;
			if (ship.getShipTarget() != null)
				target = new Vector2f(ship.getShipTarget().getLocation());
			else
				return;
			float angle = ship.getFacing();

			float rotation = MathUtils.getShortestRotation(angle, Misc.getAngleInDegrees(location, target));

			if (rotation > 0f)
				ship.giveCommand(ShipCommand.TURN_LEFT, null, 0);
			else
				ship.giveCommand(ShipCommand.TURN_RIGHT, null, 0);

			if (Math.abs(rotation) < 10) {
				float aVel = ship.getAngularVelocity() * (Math.abs(rotation) / 10);
				ship.setAngularVelocity(aVel);
			}

			if (Math.abs(rotation) < 1) {
				WeaponAPI fakeWep = engine.createFakeWeapon(ship, "hammer_single");

				MissileAPI missile = (MissileAPI) engine.spawnProjectile(ship, fakeWep, "hammer_single", location, angle, ship.getVelocity());
				missile.setWeaponSpec("hammer_single");
				engine.spawnExplosion(location, new Vector2f(0f, 0f), Color.RED, 24f, 0.5f);

				Global.getSoundPlayer().playSound("hammer_fire", 1f, 0.75f, ship.getLocation(), ship.getVelocity());

				//ship.setExplosionScale(0f);
				ship.setHitpoints(0f);
			}
		}
	}
}
