package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import contractor.data.scripts.weapons.NaniteCorrupterEveryFrame;
import org.lwjgl.util.vector.Vector2f;

public class NaniteCorrupterOnHit implements OnFireEffectPlugin, OnHitEffectPlugin {
	@Override
	public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
		if (weapon.getEffectPlugin() instanceof NaniteCorrupterEveryFrame script) {
			script.notifyFired(projectile);
		}
	}

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (projectile.getWeapon().getEffectPlugin() instanceof NaniteCorrupterEveryFrame script) {
			script.notifyHit(projectile, target, point, shieldHit);
		}
	}
}
