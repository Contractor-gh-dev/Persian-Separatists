package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepRagFighterOnHit implements OnHitEffectPlugin {
	private final static Color col1 = new Color(220, 75, 75, 125);
	private final static Color col2 = new Color(240, 75, 75, 175);
	private final DamagingExplosionSpec hullSpec = new DamagingExplosionSpec(0.25f, 16f, 16f, 5f, 5f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 10f, 0.5f, 24, col2, col1);

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		if (shieldHit)
			return;

		hullSpec.setDamageType(DamageType.HIGH_EXPLOSIVE);
		hullSpec.setUseDetailedExplosion(false);
		engine.spawnDamagingExplosion(hullSpec, projectile.getSource(), point, false);
	}
}
