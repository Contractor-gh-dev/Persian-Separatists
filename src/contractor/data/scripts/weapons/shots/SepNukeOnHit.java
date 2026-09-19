package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import contractor.data.scripts.plugins.RadiationAreaEffect;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SepNukeOnHit implements OnHitEffectPlugin, ProximityExplosionEffect {
	private static final Color col1 = new Color(20, 255, 40, 175);

	@Override
	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		doEffects(point);
	}

	@Override
	public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
		doEffects(explosion.getLocation());
	}

	private static void doEffects(Vector2f location) {
		Global.getCombatEngine().addPlugin(new RadiationAreaEffect(location));
		Global.getSoundPlayer().playSound("sep_explosion_nuke", 1f, 1f, location, new Vector2f(0f, 0f));
		Global.getCombatEngine().spawnExplosion(location, new Vector2f(0f, 0f), col1, (float) (Math.random() * 64f + 512f), 3f);
		Global.getCombatEngine().spawnExplosion(location, new Vector2f(0f, 0f), col1, (float) (Math.random() * 128f + 800f), 3f);
		Global.getCombatEngine().spawnExplosion(location, new Vector2f(0f, 0f), col1, (float) (Math.random() * 64f + 1200f), 1.5f);
		Global.getCombatEngine().spawnExplosion(location, new Vector2f(0f, 0f), col1, (float) (Math.random() * 128f + 1600f), 1f);
	}
}
