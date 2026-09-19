package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepMk74OnHit implements OnHitEffectPlugin {
	private final Color col1 = new Color(210, 120, 70, 255);
	private final Color col2 = new Color(240, 100, 50, 220);
	private final DamagingExplosionSpec spec = new DamagingExplosionSpec(0.05f, 16f, 10f, 20f, 15f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			1f, 7f, 0.4f, 32, col1, col2);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		spec.setDamageType(DamageType.FRAGMENTATION);
		spec.setUseDetailedExplosion(false);
		engine.spawnDamagingExplosion(spec, projectile.getSource(), point, false);
	}
}
