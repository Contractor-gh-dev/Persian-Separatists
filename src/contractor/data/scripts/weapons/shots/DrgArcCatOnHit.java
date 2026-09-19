package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class DrgArcCatOnHit implements OnHitEffectPlugin {
	public static final float DAMAGE = 5f;

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		engine.applyDamage(target, point, DAMAGE, DamageType.HIGH_EXPLOSIVE, 0f, false, false, projectile.getSource(), false);
	}
}
