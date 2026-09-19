package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepMk4OnHit implements OnHitEffectPlugin {
	private final Color col1 = new Color(200, 140, 0, 255);
	private final Color col2 = new Color(220, 100, 0, 220);
	private final DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 70f, 40f, 100f, 50f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 12f, 1f, 80, col1, col2);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		spec.setDamageType(DamageType.FRAGMENTATION);
		spec.setUseDetailedExplosion(true);
		spec.setDetailedExplosionFlashRadius(40f);
		spec.setDetailedExplosionRadius(80f);
		engine.spawnDamagingExplosion(spec, projectile.getSource(), point, true);
	}
}
