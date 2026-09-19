package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DrgNeedleListener implements AdvanceableListener {
	private float delay = 1f;
	private boolean playSound = true;
	private final CombatEngineAPI engine;
	private final ShipAPI target;
	private final float time = 5f;
	private static final Color col1 = new Color(170, 30, 110, 255);
	private static final Color col2 = new Color(180, 25, 100, 255);
	public final int burstLimit = 3;
	public static final DamagingExplosionSpec HULLSPEC = new DamagingExplosionSpec(0.5f, 48f, 24f, 300f, 150f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 12f, 1.0f, 32, col2, col1);
	static {
		HULLSPEC.setDamageType(DamageType.KINETIC);
	}

	private final DamagingExplosionSpec hullSpecBurst = new DamagingExplosionSpec(0.5f, 48f, 24f, 400f, 200f, CollisionClass.PROJECTILE_FF, CollisionClass.PROJECTILE_FIGHTER,
			4f, 16f, 1.5f, 48, col2, col1);
	private final List<NeedleManager> needles = new ArrayList<>();

	private static class NeedleManager {
		private final float needleAngle, initialAngle, texX, texY;
		private float time;
		private final Vector2f pos;
		private final ShipAPI source;
		private final SpriteAPI sprite;
		private Vector2f computedPos;

		protected NeedleManager(float time, float needleAngle, float targetAngle, Vector2f location, ShipAPI source) {
			this.time = time;
			this.needleAngle = needleAngle;
			this.initialAngle = targetAngle;
			this.pos = new Vector2f(location);
			this.source = source;
			this.sprite = Global.getSettings().getSprite("systems", "drg_needle_sprite");
			this.texX = sprite.getWidth();
			this.texY = sprite.getHeight();
		}
	}

	public DrgNeedleListener(float needleAngle, float targetAngle, Vector2f location, ShipAPI target, ShipAPI source) {
		needles.add(new NeedleManager(time, needleAngle, targetAngle, location, source));
		this.target = target;
		this.engine = Global.getCombatEngine();
	}

	@Override
	public void advance(float amount) {
		if (target == null)
			return;

//        for (NeedleManager n : needles) {
//            SpriteAPI sprite = Global.getSettings().getSprite("systems", "drg_needle_sprite");
//
//            Vector2f toRotate = new Vector2f(n.pos);
//            VectorUtils.rotate(toRotate, target.getFacing() - n.initialAngle);
//            n.computedPos = new Vector2f(Vector2f.add(toRotate, target.getLocation(), null));
//            float x = n.computedPos.getX();
//            float y = n.computedPos.getY();
//
//            float angle = n.needleAngle + n.initialAngle + target.getFacing() - 90f;
//
//            sprite.setSize(n.texX, n.texY);
//            sprite.setColor(Color.white);
//            sprite.setAngle(angle);
//            sprite.setAdditiveBlend();
//            sprite.setCenter(0f, 0f);
//            sprite.renderAtCenter(x, y);
//        }

		for (NeedleManager n : needles) {
			Vector2f toRotate = new Vector2f(n.pos);
			VectorUtils.rotate(toRotate, target.getFacing() - n.initialAngle);
			n.computedPos = new Vector2f(Vector2f.add(toRotate, target.getLocation(), null));

			engine.addSmoothParticle(n.computedPos, new Vector2f((float) (20f - (Math.random() * 40f)), (float) (20f - (Math.random() * 40f))), 14f, 0.7f, 0.4f, col1);

			MagicRender.battlespace(n.sprite, n.computedPos, new Vector2f(), new Vector2f(n.texX * 1.05f, n.texY * 1.05f), null, n.needleAngle + n.initialAngle + target.getFacing() - 90f,
					0f, Color.white, true, 0f, 0f, 0f, 0f, 0f, 0f, 0.0167f, 0f, CombatEngineLayers.BELOW_PHASED_SHIPS_LAYER);
		}

		if (needles.size() >= burstLimit) {
			delay -= amount;
			if (playSound) {
				Global.getSoundPlayer().playSound("system_nova_burst_fire", 0.8f, 1.5f, target.getLocation(), target.getVelocity());
				playSound = false;
			}
			if (delay <= 0) {
				for (NeedleManager n : needles) {
					Global.getSoundPlayer().playSound("rift_lightning_explosion", 1.1f, 0.6f, n.computedPos, target.getVelocity());

					engine.applyDamage(target, n.computedPos, 400f, DamageType.ENERGY, 100f, true, false, n.source, false);
					engine.spawnExplosion(n.computedPos, new Vector2f(0f, 0f), col2, 160f, 1.5f);
				}
				needles.clear();
				target.removeListener(this);
				return;
			}
		}

		if (needles.size() >= burstLimit)
			return;

		int j = needles.size();
		for (int i = j - 1; i >= 0; i--) {
			NeedleManager n = needles.get(i);
			n.time -= amount;
			if (n.time <= 0) {
				engine.applyDamage(target, n.computedPos, 300f, DamageType.KINETIC, 50f, true, false, n.source, false);
				engine.spawnExplosion(n.computedPos, new Vector2f(0f, 0f), col1, 128f, 1f);
				Global.getSoundPlayer().playSound("rift_lightning_explosion", 1.2f, 0.5f, n.computedPos, target.getVelocity());
				n = null;
				needles.remove(i);
			}
		}

		if (needles.isEmpty())
			target.removeListener(this);
	}

	public void addNeedle(float needleAngle, float targetAngle, Vector2f location, ShipAPI source) {
		needles.add(new NeedleManager(time, needleAngle, targetAngle, location, source));
	}

	public void addNeedle(float timeOvrd, float needleAngle, float targetAngle, Vector2f location, ShipAPI source) {
		needles.add(new NeedleManager(time, needleAngle, targetAngle, location, source));
	}
}
