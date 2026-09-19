package contractor.data.scripts.weapons.shots;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.combat.entities.terrain.Asteroid;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class SepTermiteOnHit implements OnHitEffectPlugin {
	private static final java.awt.Color col1 = new Color(220, 105, 75, 125);
	private static final java.awt.Color col2 = new Color(240, 105, 75, 175);
	private static final DamagingExplosionSpec shieldSpec = new DamagingExplosionSpec(0.5f, 16f, 12f, 50f, 25f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			2f, 10f, 0.5f, 12, col2, col1);
	private static final DamagingExplosionSpec hullSpec = new DamagingExplosionSpec(0.5f, 32f, 24f, 100f, 50f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 12f, 0.75f, 24, col2, col1);
	private static final DamagingExplosionSpec fighterSpec = new DamagingExplosionSpec(0.5f, 24f, 16f, 200f, 100f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 12f, 0.75f, 24, col2, col1);
	private static final DamagingExplosionSpec astSpec = new DamagingExplosionSpec(0.5f, 16f, 8f, 400f, 100f, CollisionClass.ASTEROID, CollisionClass.NONE,
			4f, 10f, 0.75f, 16, col2, col1);

	public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
		ShipAPI source = projectile.getSource();
		if (source == null)
			return;
		if (shieldHit) {
			shieldSpec.setDamageType(DamageType.FRAGMENTATION);
			shieldSpec.setUseDetailedExplosion(false);
			engine.spawnDamagingExplosion(shieldSpec, source, point, false);
			return;
		}
		if (target == null || target.isExpired() || target.wasRemoved())
			return;

		boolean isAsteroid = target instanceof Asteroid;
		boolean isFighter = target instanceof ShipAPI s && s.isFighter();

		engine.addPlugin(new EveryFrameCombatPlugin() {
			final boolean asteroid = isAsteroid;
			final boolean fighter = isFighter;
			final CombatEntityAPI targ = target;
			final IntervalUtil tracker = new IntervalUtil(0.75f, 1f);
			final Vector2f hitInShipCoords = new Vector2f(Vector2f.sub(point, targ.getLocation(), null));
			final float initialFacing = targ.getFacing();

			@Override
			public void advance(float amount, List<InputEventAPI> events) {
				if (engine.isPaused())
					return;

				boolean done = false;

				tracker.advance(amount);
				if (tracker.intervalElapsed()) {
					if (targ.wasRemoved()) {
						engine.removePlugin(this);
						return;
					}
					done = true;
					VectorUtils.rotate(hitInShipCoords, targ.getFacing() - initialFacing);
					Vector2f finalHitPos = new Vector2f(Vector2f.add(hitInShipCoords, targ.getLocation(), null));

					if (asteroid) {
						astSpec.setDamageType(DamageType.FRAGMENTATION);
						astSpec.setUseDetailedExplosion(false);
						engine.spawnDamagingExplosion(astSpec, source, finalHitPos, false);
					} else if (fighter) {
						hullSpec.setDamageType(DamageType.FRAGMENTATION);
						hullSpec.setUseDetailedExplosion(false);
						engine.spawnDamagingExplosion(fighterSpec, source, finalHitPos, false);
					} else {
						hullSpec.setDamageType(DamageType.FRAGMENTATION);
						hullSpec.setUseDetailedExplosion(false);
						engine.spawnDamagingExplosion(hullSpec, source, finalHitPos, false);
					}
				}
				if (done)
					engine.removePlugin(this);
			}

			public void renderInWorldCoords(ViewportAPI viewport) {
			}

			public void renderInUICoords(ViewportAPI viewport) {
			}

			public void init(CombatEngineAPI engine) {
			}

			public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
			}
		});
	}
}
