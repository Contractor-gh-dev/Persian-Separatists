package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class DrgDemonstarEveryFrame implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
	private boolean firing = false;
	private int shotCount = 0;
	private float delay = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (firing)
			if (weapon.getCooldownRemaining() == 0) {
				delay += amount;
			} else
				delay = 0f;

		float frameBuffer = engine.getElapsedInLastFrame();
		if (delay >= 0.26f + frameBuffer) {
			firing = false;
			delay = 0f;
		}

		if (shotCount >= 30) {
			engine.addPlugin(new delayedShotsScript(engine, weapon.getShip(), weapon, (int) Math.floor(shotCount / 7.5d)));
			shotCount = 0;
		}
		if (!firing)
			if (shotCount < 7.5f)
				shotCount = 0;
			else {
				engine.addPlugin(new delayedShotsScript(engine, weapon.getShip(), weapon, (int) Math.floor(shotCount / 7.5d)));
				shotCount = 0;
			}
	}

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		shotCount++;
		firing = true;
	}

	private static class delayedShotsScript implements EveryFrameCombatPlugin {
		private final ShipAPI ship;
		private final WeaponAPI weapon;
		private final CombatEngineAPI engine;
		private final int shots;
		private int count = 0;
		private float timer = 0f;

		delayedShotsScript(CombatEngineAPI engine, ShipAPI ship, WeaponAPI weapon, int shots) {
			this.engine = engine;
			this.ship = ship;
			this.weapon = weapon;
			this.shots = shots;
		}

		@Override
		public void advance(float amount, List<InputEventAPI> events) {
			timer += amount;

			if (timer > 0.15f) {
				timer = 0f;
				count++;
				WeaponAPI fakeWep = engine.createFakeWeapon(ship, "kineticblaster");
				engine.spawnProjectile(ship, fakeWep, fakeWep.getId(), weapon.getFirePoint(0), weapon.getCurrAngle(), weapon.getShip().getVelocity());
				Global.getSoundPlayer().playSound("kinetic_blaster_fire", 1f, 1f, weapon.getLocation(), ship.getVelocity());
			}

			if (count >= shots)
				engine.removePlugin(this);
		}

		public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
		}

		public void renderInWorldCoords(ViewportAPI viewport) {
		}

		public void renderInUICoords(ViewportAPI viewport) {
		}

		public void init(CombatEngineAPI engine) {
		}
	}
}
