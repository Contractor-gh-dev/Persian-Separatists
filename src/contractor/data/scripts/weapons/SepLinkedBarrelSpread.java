package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

public class SepLinkedBarrelSpread implements OnFireEffectPlugin {
	public static final float spreadRatio = 3f;

	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		LinkOnFireBarrelSpread(weapon);
	}

	public static void LinkOnFireBarrelSpread(WeaponAPI weapon) {
		weapon.ensureClonedSpec();
		WeaponSpecAPI spec = weapon.getSpec();
		float angleClamped = Math.max(spec.getMaxSpread() / spreadRatio, 1.5f);
		float currSpread = weapon.getCurrSpread() * 0.5f;
		if (currSpread < 1)
			currSpread = 1;
		int angleSpaces = Math.round(angleClamped * currSpread);

		spec.getTurretAngleOffsets().replaceAll(ignored -> (float) (((Math.random() - 0.5d) * angleSpaces) / angleClamped));
		spec.getHardpointAngleOffsets().replaceAll(ignored -> (float) (((Math.random() - 0.5d) * angleSpaces) / angleClamped));

		//spec.getTurretAngleOffsets().replaceAll(ignored -> (Math.round((Math.random() - 0.5d) * angleSpaces)) / angleClamped);
		//spec.getHardpointAngleOffsets().replaceAll(ignored -> (Math.round((Math.random() - 0.5d) * angleSpaces)) / angleClamped);
	}
}
