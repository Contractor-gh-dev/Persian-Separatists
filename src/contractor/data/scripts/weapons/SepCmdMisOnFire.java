package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.util.CommandMissileInterface;

public class SepCmdMisOnFire implements OnFireEffectPlugin {
	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		MissileAPI missile = (MissileAPI) projectile;
		CommandMissileInterface plugin = (CommandMissileInterface) weapon.getEffectPlugin();
		plugin.appendMissile(missile);
	}
}
