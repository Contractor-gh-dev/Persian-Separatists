package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import contractor.data.scripts.statuseffects.StatusEffect.StatusType;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class RadiationAreaEffect implements EveryFrameCombatPlugin {
	private final Vector2f center;
	private static final Color RAD_GLOW = new Color(50, 255, 40, 200);
	private float time = 20f;
	private float radius = 512f;

	public RadiationAreaEffect(Vector2f center) {
		this.center = new Vector2f(center);
	}

	public RadiationAreaEffect(Vector2f center, float time) {
		this.center = new Vector2f(center);
		this.time = time;
	}

	public RadiationAreaEffect(float x, float y) {
		center = new Vector2f(x, y);
	}

	public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
	}

	@Override
	public void advance(float amount, List<InputEventAPI> events) {
		radius += time * 0.04f;

		for (int i = 0; i < 20; i++) {
			Vector2f randomArea = new Vector2f(MathUtils.getRandomPointInCircle(center, radius));
			Vector2f vel = new Vector2f((float) Math.random() * 16f, (float) Math.random() * 16f);
			float size = (float) Math.random() * 8f + 4f;
			Global.getCombatEngine().addHitParticle(randomArea, vel, size, 0.75f, 0.1f, 0.25f, RAD_GLOW);

			randomArea = new Vector2f(MathUtils.getRandomPointOnCircumference(center, radius));
			vel = new Vector2f(0f, 0f);
			size = (float) Math.random() * 6f + 4f;
			Global.getCombatEngine().addHitParticle(randomArea, vel, size, 0.75f, 0.1f, 0.25f, RAD_GLOW);
		}

		List<ShipAPI> ships = CombatUtils.getShipsWithinRange(center, radius * 0.925f);

		for (ShipAPI ship : ships) {
			if (ship.getCollisionClass() == CollisionClass.NONE)
				continue;
			StatusEffectUtils.addOrMaintainEffect(ship, StatusType.RADIATION, amount * 1.5f, 2f);
		}

		time -= amount;
		if (time <= 0f)
			Global.getCombatEngine().removePlugin(this);
	}

	public void renderInWorldCoords(ViewportAPI viewport) {
	}

	public void renderInUICoords(ViewportAPI viewport) {
	}

	public void init(CombatEngineAPI engine) {
	}
}
