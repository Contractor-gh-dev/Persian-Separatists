package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class Entropic extends BaseStatusEffect {
	private float radius, timer = 0f;
	private final Color under = new Color(180, 50, 40, 200);
	private final Color over = new Color(200, 75, 10, 255);

	public Entropic(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.id = target.getId() + StatusType.ENTROPIC;
		this.type = StatusType.ENTROPIC;
		radius = target.getSpriteAPI().getWidth();
		radius += target.getSpriteAPI().getHeight();
		radius *= 0.2f;
	}

	public void advance(float amount) {
		if (!target.isAlive()) {
			remove(false);
			return;
		}

		float strength = MathUtils.clamp(effectLevel / 7.5f, 0.1f, 0.8f);
		target.setJitterUnder(this, under, strength, 10, 0.2f, 9f);
		target.setJitter(this, over, strength, 3, 0, 18f);
		boolean isPlayer = target == Global.getCombatEngine().getPlayerShip();
		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;

		timer += amount * timeMult;
		if (timer > 0.1f) {
			timer = 0f;
			if (Math.random() > 0.9d) {
				List<ShipEngineAPI> engines = target.getEngineController().getShipEngines();
				List<WeaponAPI> weapons = target.getUsableWeapons();

				double chance = Math.random();
				if (chance < 0.45d) {
					if (!engines.isEmpty()) {
						int select = MathUtils.clamp((int) (Math.random() * engines.size()), 0, engines.size() - 1);
						engines.get(select).disable();
						Global.getCombatEngine().spawnExplosion(engines.get(select).getLocation(), target.getVelocity(), under, 32f, 0.75f);
					} else
						doExplosion();
				} else if (chance < 0.90d) {
					if (!weapons.isEmpty()) {
						int select = MathUtils.clamp((int) (Math.random() * weapons.size()), 0, weapons.size() - 1);
						weapons.get(select).disable();
						Global.getCombatEngine().spawnExplosion(weapons.get(select).getLocation(), target.getVelocity(), under, 32f, 0.75f);
					} else
						doExplosion();
				} else {
					doExplosion();
				}
			}
		}

		if (isPlayer)
			Global.getCombatEngine().maintainStatusForPlayerShip(id, Global.getSettings().getSpriteName("systems", "drg_entropic_ui_sprite"), "Entropic Instability", "Malfunctions Possible", true);

		effectLevel -= amount * timeMult;
		if (effectLevel <= 0f)
			remove(false);
	}

	public void addEffectLevel(float effectLevel) {
		this.effectLevel += effectLevel;
		if (this.effectLevel > 30f)
			this.effectLevel = 30f;
	}

	private void doExplosion() {
		Vector2f loc = MathUtils.getRandomPointInCircle(target.getLocation(), radius);
		CombatEngineAPI engine = Global.getCombatEngine();
		engine.applyDamage(target, loc, 100f, DamageType.ENERGY, 0f, true, false, null, true);
		engine.spawnExplosion(loc, target.getVelocity(), under, 64f, 1f);
		engine.addFloatingText(loc, "Malfunction Damage", 16f, Misc.getNegativeHighlightColor(), target, 1f, 0f);
	}
}
