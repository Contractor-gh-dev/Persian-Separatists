package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import contractor.data.scripts.weapons.shots.SepAndrossMissileAI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class SepAndrossIntercept extends BaseShipSystemScript {
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
		} else {
			return;
		}

		if (ship.getEngineController().isFlamedOut() || !ship.isAlive())
			return;

		int ammo = 0;

		for (WeaponAPI w : ship.getAllWeapons()) {
			if (!w.isDecorative())
				ammo += w.getAmmo();
		}

		if (ammo <= 2 || ship.getHullLevel() <= 0.2f) {
			CombatEngineAPI engine = Global.getCombatEngine();
			//FighterWingAPI wing = ship.getWing();
			Vector2f location = new Vector2f(ship.getLocation());
			float angle = ship.getFacing();

			WeaponAPI fakeWep = engine.createFakeWeapon(ship, "sep_andross_missile");

			MissileAPI missile = (MissileAPI) engine.spawnProjectile(ship, fakeWep, "sep_andross_missile", location, angle, ship.getVelocity());
			missile.setWeaponSpec("sep_andross_missile");
			missile.setEmpResistance(500);
			missile.setMissileAI(new SepAndrossMissileAI(missile, ship));
			engine.spawnExplosion(location, new Vector2f(0f, 0f), Color.RED, 24f, 0.5f);

			ship.setExplosionScale(0f);
			ship.setHitpoints(0f);
		}
	}
}
