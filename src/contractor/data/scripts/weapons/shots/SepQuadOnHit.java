package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepQuadOnHit implements OnHitEffectPlugin {
	private final static java.awt.Color col2 = new Color(240, 75, 75, 175);

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		engine.applyDamage(target, point, 20f, DamageType.HIGH_EXPLOSIVE, 0f, false, false, projectile.getSource());
		engine.spawnExplosion(point, new Vector2f(0f, 0f), col2, 16f, 0.25f);
	}
}
