package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Drg3PCOnHit implements OnHitEffectPlugin {
	private final Color col1 = new Color(150, 40, 180, 255);
	private final Color col2 = new Color(80, 20, 210, 255);
	private final DamagingExplosionSpec spec = new DamagingExplosionSpec(0.1f, 90f, 60f, 100f, 50f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 12f, 2f, 120, col1, col2);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		spec.setDamageType(DamageType.KINETIC);
		spec.setUseDetailedExplosion(true);
		spec.setDetailedExplosionFlashRadius(75f);
		spec.setDetailedExplosionRadius(110f);
		engine.spawnDamagingExplosion(spec, projectile.getSource(), point, true);
	}
}
