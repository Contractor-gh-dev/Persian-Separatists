package contractor.data.scripts.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SepFCRGListener implements AdvanceableListener, SepFCRGListenerInterface {
	private final List<EffectManager> managerList = new ArrayList<>();
	private final ShipAPI target;
	private final CombatEngineAPI engine;
	private final IntervalUtil interval = new IntervalUtil(0.08f, 0.2f);
	private final IntervalUtil intervalTwo = new IntervalUtil(0.15f, 0.3f);
	private static final java.awt.Color col1 = new Color(150, 15, 5);
	private static final java.awt.Color col2 = new Color(255, 160, 200);
	private static final java.awt.Color col3 = new Color(120, 10, 0);
	private static final EmpArcEntityAPI.EmpArcParams PARAMS = new EmpArcEntityAPI.EmpArcParams(); static {
		PARAMS.zigZagReductionFactor = 2f;
		PARAMS.segmentLengthMult = 20f;
		PARAMS.minFadeOutMult = 1.75f;
		PARAMS.flickerRateMult = 0.8f;
		PARAMS.glowSizeMult = 0.5f;
	}

	private static class EffectManager {
		private final Vector2f point;
		private float timer = 2.5f;

		EffectManager(Vector2f point) {
			this.point = point;
		}
	}

	public SepFCRGListener(Vector2f point, ShipAPI target) {
		this.target = target;
		this.engine = Global.getCombatEngine();
		Vector2f localPoint = Vector2f.sub(point, target.getLocation(), null);
		VectorUtils.rotate(localPoint, -target.getFacing());
		managerList.add(new EffectManager(localPoint));
	}

	@Override
	public void advance(float amount) {
		if (managerList.isEmpty())
			return;
		boolean isPlayer = target == engine.getPlayerShip();
		float timeMult;
		if (isPlayer)
			timeMult = 1f;
		else
			timeMult = target.getMutableStats().getTimeMult().modified;
		float scaledAmount = amount * timeMult;
		interval.advance(scaledAmount);
		intervalTwo.advance(scaledAmount);

		int i = managerList.size() - 1;
		for (; i >= 0; i--) {
			Vector2f mapPoint = new Vector2f(managerList.get(i).point);
			VectorUtils.rotate(mapPoint, target.getFacing());
			Vector2f.add(mapPoint, target.getLocation(), mapPoint);

			if (interval.intervalElapsed()) {
				Vector2f point = MathUtils.getRandomPointInCircle(mapPoint, 24f);
				Vector2f vel = new Vector2f((float) ((Math.random() - 0.5f) * 24f), (float) (Math.random() - 0.5f) * 24f);
				engine.addNegativeParticle(point, vel, (float) (Math.random() * 6f + 6f), 0.5f, (float) Math.random() + 0.5f, col1);

				point = MathUtils.getRandomPointInCircle(mapPoint, 24f);
				vel = new Vector2f((float) ((Math.random() - 0.5f) * 24f), (float) (Math.random() - 0.5f) * 24f);
				engine.addSwirlyNebulaParticle(point, vel, (float) (Math.random() * 12f + 20f), (float) (Math.random() + 0.8f), 0.6f, 0.8f, (float) Math.random() + 0.5f, col1, false);
				engine.addNegativeNebulaParticle(point, vel, (float) (Math.random() * 6f + 10f), (float) (Math.random() + 0.7f), 0.6f, 0.8f, (float) Math.random() + 0.5f, col3);

				point = MathUtils.getRandomPointInCircle(mapPoint, 12f);
				vel = new Vector2f((float) ((Math.random() - 0.5f) * 24f), (float) (Math.random() - 0.5f) * 24f);
				engine.addSwirlyNebulaParticle(point, vel, (float) (Math.random() * 16f + 24f), (float) (Math.random() + 0.9f), 0.6f, 0.8f, (float) Math.random() + 0.5f, col1, true);

				point = MathUtils.getRandomPointInCircle(mapPoint, 24f);
				vel = new Vector2f((float) ((Math.random() - 0.5f) * 24f), (float) (Math.random() - 0.5f) * 24f);
				engine.addHitParticle(point, vel, (float) (Math.random() * 6f + 18f), (float) (Math.random() + 0.5f), 0.75f, col1);
				vel = new Vector2f((float) ((Math.random() - 0.5f) * 24f), (float) (Math.random() - 0.5f) * 24f);
				engine.addSmoothParticle(point, vel, (float) (Math.random() * 8f + 20f), (float) (Math.random() + 0.5f), 0.8f, col1);
			}
			if (intervalTwo.intervalElapsed()) {
				engine.spawnEmpArc(target, mapPoint, target, target, DamageType.ENERGY, 0f, 12.5f, target.getCollisionRadius(), "riftcascade_windup", (float) (Math.random() * 10f + 16f),
						col1, col2, PARAMS);
			}

			managerList.get(i).timer -= scaledAmount;
			if (managerList.get(i).timer <= 0f)
				managerList.remove(i);
		}
	}

	@Override
	public void reportHit(Vector2f point) {
		Vector2f localPoint = Vector2f.sub(point, target.getLocation(), null);
		VectorUtils.rotate(localPoint, -target.getFacing());
		if (managerList.isEmpty())
			managerList.add(new EffectManager(localPoint));
		else {
			int i = managerList.size() - 1;
			for (; i >= 0; i--) {
				if (MathUtils.isWithinRange(localPoint, managerList.get(i).point, 24f)) {
					if (managerList.get(i).timer < 6f)
						managerList.get(i).timer += 0.5f;
				} else
					managerList.add(new EffectManager(localPoint));
			}
		}
	}
}
