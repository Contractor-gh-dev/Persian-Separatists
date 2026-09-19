package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class Radiation extends BaseStatusEffect {
	private float radius;
	private static final Color radGlow = new Color(50, 255, 40, 200);

	public Radiation(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.type = StatusType.RADIATION;
		radius = target.getSpriteAPI().getWidth();
		radius += target.getSpriteAPI().getHeight();
		radius *= 0.25f;
	}

	public void advance(float amount) {
		if (!target.isAlive()) {
			remove();
			return;
		}

		Vector2f randomArea = new Vector2f(MathUtils.getRandomPointInCircle(target.getLocation(), radius));
		float size = (float) Math.random() * 16f + 8f;
		Global.getCombatEngine().addHitParticle(randomArea, target.getVelocity(), size, 0.75f, 0.5f, radGlow);

		boolean isPlayer = target == Global.getCombatEngine().getPlayerShip();

		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;

		float time = target.getTimeDeployedForCRReduction();
		time += amount * 0.5f * timeMult;

		target.setTimeDeployed(time);

		if (target == Global.getCombatEngine().getPlayerShip())
			Global.getCombatEngine().maintainStatusForPlayerShip(id, Global.getSettings().getSpriteName("systems", "drg_rad_ui_sprite"), "Radioactive Decay", "CR LOSS INCREASED: " + Math.round(effectLevel), true);

		effectLevel -= amount * timeMult;
		if (effectLevel <= 0f)
			remove();
	}
}
