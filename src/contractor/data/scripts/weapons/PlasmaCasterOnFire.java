package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.weapons.shots.PlasmaCasterEnergyAI;
import contractor.data.scripts.weapons.shots.PlasmaCasterMissileAI;

public class PlasmaCasterOnFire implements OnFireEffectPlugin {
	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MissileAPI missile = (MissileAPI) projectile;
		ShipAPI ship = weapon.getShip();
		if (ship == null)
			return;

		if (weapon.getSlot().getWeaponType().equals(WeaponAPI.WeaponType.ENERGY)) {
			WeaponAPI wep = engine.createFakeWeapon(ship, "drg_plasmacaster_hidden");
			MissileAPI missileClone = (MissileAPI) engine.spawnProjectile(ship, wep, "drg_plasmacaster_hidden", weapon.getFirePoint(0), weapon.getCurrAngle(), ship.getVelocity());
			missileClone.setMissileAI(new PlasmaCasterEnergyAI(missile, ship));

			engine.removeEntity(missile);
		} else
			missile.setMissileAI(new PlasmaCasterMissileAI(missile, ship));

	}
}
