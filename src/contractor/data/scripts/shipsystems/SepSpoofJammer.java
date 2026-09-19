package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SepSpoofJammer extends BaseShipSystemScript {
	public static final float DEBUFF = 25f, BASE_RANGE = 2200f;
	private static final float FADE_TIME = 0.15f, HOLD_TIME = 0.2f;
	private static final Color COL = new Color(255, 255, 255);
	private static final Color JITTER1 = new Color(40, 50, 150);
	private static final Color JITTER2 = new Color(20, 60, 210);
	private final IntervalUtil interval = new IntervalUtil(0.2f, 0.4f);
	private final List<DroneData> FXDrones = new ArrayList<>();

	protected static class DroneData {
		protected float time;
		protected final float angle;
		protected final ShipAPI drone;
		protected final Vector2f point;

		DroneData(ShipAPI drone, float time, Vector2f point, float angle) {
			this.drone = drone;
			this.time = time;
			this.point = point;
			this.angle = angle;
		}
	}

	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		if (stats.getEntity() instanceof ShipAPI ship) {
			float maxRange = stats.getSystemRangeBonus().computeEffective(BASE_RANGE);
			float maxRangeSQ = maxRange * maxRange;
			stats.getMaxSpeed().modifyPercent(id, -DEBUFF);
			stats.getMaxTurnRate().modifyPercent(id, -DEBUFF);
			if (ship == Global.getCombatEngine().getPlayerShip()) {
				ShipSystemSpecAPI sysSpec = ship.getSystem().getSpecAPI();
				Global.getCombatEngine().maintainStatusForPlayerShip(sysSpec.getId(), sysSpec.getIconSpriteName(), sysSpec.getName(), "Speed Reduced: -" + (int) DEBUFF + "%", true);
			}
			if (state == State.IN || state == State.OUT) {
				ship.setJitter(this, JITTER1, effectLevel * 0.66f, 3, 0f, effectLevel * 6f);
				ship.setJitterUnder(this, JITTER2, effectLevel * 0.66f, 6, 1f, effectLevel * 6f * 2f);
				if (state == State.OUT)
					unapply(stats, id);
			} else if (state == State.ACTIVE) {
				ship.setJitter(this, JITTER1, effectLevel * 0.5f, 3, 0f, effectLevel * 4f);
				ship.setJitterUnder(this, JITTER2, effectLevel * 0.5f, 6, 1f, effectLevel * 4f * 2f);

				interval.advance(Global.getCombatEngine().getElapsedInLastFrame());
				if (interval.intervalElapsed()) {
					float threeQuarter = maxRange * 0.75f;
					float oneThird = maxRange * 0.33f;
					List<ShipAPI> allShips = Global.getCombatEngine().getShips();
					List<ShipAPI> targets = new ArrayList<>();

					for (ShipAPI s : allShips) {
						if (s.getOwner() == ship.getOwner())
							continue;
						if (s.isFighter() || s.isHulk())
							continue;
						float dist = MathUtils.getDistanceSquared(ship.getLocation(), s.getLocation());
						if (dist < (maxRangeSQ))
							targets.add(s);
					}
					if (targets.isEmpty()) {
						int count = (int) (Math.random() * 4f);
						List<Vector2f> fakePoints = new ArrayList<>();
						float[] goalAngles = new float[count];

						for (int i = 0; i < count; i++) {
							float min, max;
							min = Misc.normalizeAngle(ship.getFacing() + 75f);
							max = Misc.normalizeAngle(ship.getFacing() - 75f);
							fakePoints.add(MathUtils.getRandomPointInCone(ship.getLocation(), maxRange - 200f, min, max));
							goalAngles[i] = Misc.getAngleInDegrees(ship.getLocation(), fakePoints.get(i));
						}
						for (int i = 0; i < count; i++) {
							float range = Math.min(oneThird + ((float) Math.random() * threeQuarter), MathUtils.getDistance(ship, fakePoints.get(i)));
							Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), range, goalAngles[i]);
							float ghostAngle = Misc.normalizeAngle(Misc.getAngleInDegrees(point, fakePoints.get(i)) - 90f);

							renderFX(ship, point, ghostAngle);
							//FXDrones.add(i, createFXDrone(ship, point, ghostAngle));
						}
					} else {
						float[] goalAngles = new float[targets.size()];

						for (int i = 0; i < targets.size(); i++)
							goalAngles[i] = Misc.getAngleInDegrees(ship.getLocation(), targets.get(i).getLocation());

						for (int i = 0; i < targets.size(); i++) {
							float range = Math.min(oneThird + ((float) Math.random() * threeQuarter), MathUtils.getDistance(ship, targets.get(i)) * 0.9f);
							Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), range, (goalAngles[i] + ((float) (Math.random() - 0.5f) * 60f)));
							float ghostAngle = Misc.normalizeAngle(Misc.getAngleInDegrees(point, targets.get(i).getLocation()) - 90f);

							renderFX(ship, point, ghostAngle);
							FXDrones.add(i, createFXDrone(ship, point, ghostAngle));
						}
					}
				}
				for (int i = FXDrones.size() - 1; i >= 0; i--) {
					manageFXDrone(i, ship);
				}
			}
		}
	}

	private DroneData createFXDrone(ShipAPI ship, Vector2f point, float angle) {
		ShipHullSpecAPI spec = Global.getSettings().getHullSpec("dem_drone");
		ShipVariantAPI v = Global.getSettings().createEmptyVariant("dem_drone", spec);
		ShipAPI FXDrone;
		FXDrone = Global.getCombatEngine().createFXDrone(v);
		FXDrone.setLayer(CombatEngineLayers.ABOVE_SHIPS_AND_MISSILES_LAYER);
		FXDrone.setOwner(ship.getOriginalOwner());
		FXDrone.setDrone(true);
		FXDrone.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DRONE_MOTHERSHIP, 100000f, ship);
		FXDrone.setCollisionClass(CollisionClass.NONE);
		Global.getCombatEngine().addEntity(FXDrone);

		return new DroneData(FXDrone, FADE_TIME * 2f + HOLD_TIME, point, angle);
	}

	private void manageFXDrone(int index, ShipAPI ship) {
		if (FXDrones.get(index) != null && FXDrones.get(index).drone != null) {
			FXDrones.get(index).time -= Global.getCombatEngine().getElapsedInLastFrame();
			ShipAPI FXDrone = FXDrones.get(index).drone;
			if (FXDrone != null) {
				FXDrone.setOwner(ship.getOwner());
				FXDrone.getLocation().set(FXDrones.get(index).point);
				FXDrone.setFacing(FXDrones.get(index).angle);
				FXDrone.getVelocity().set(new Vector2f(0f, 0f));
				FXDrone.setAngularVelocity(0f);
			}
		}
		if (FXDrones.get(index).time <= 0f) {
			Global.getCombatEngine().removeEntity(FXDrones.get(index).drone);
			FXDrones.remove(index);
		}
	}

	private void renderFX(ShipAPI ship, Vector2f point, float ghostAngle) {
		SpriteAPI sprite = Global.getSettings().getSprite("systems", ship.getHullSpec().getBaseHullId() + "_sprite");
		Vector2f spriteSize = new Vector2f(sprite.getWidth(), sprite.getHeight());
		Vector2f zeroed = new Vector2f(0f, 0f);
		MagicRender.battlespace(sprite, point, zeroed, spriteSize, zeroed, ghostAngle, 0f, COL, true, 8f, 4f, 0.2f,
				0.25f, 0.05f, FADE_TIME, HOLD_TIME, FADE_TIME, CombatEngineLayers.BELOW_INDICATORS_LAYER);
		spriteSize = new Vector2f(sprite.getWidth() * 1.05f, sprite.getHeight() * 1.05f);
		MagicRender.battlespace(sprite, point, zeroed, spriteSize, zeroed, ghostAngle, 0f, JITTER2, true, 8f, 4f, 0.25f,
				0.4f, 0.05f, FADE_TIME, HOLD_TIME, FADE_TIME, CombatEngineLayers.BELOW_INDICATORS_LAYER);
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		for (DroneData data : FXDrones)
			Global.getCombatEngine().removeEntity(data.drone);
		FXDrones.clear();
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isActive())
			return "JAMMING";
		else if (system.isChargeup())
			return "CHARGING";
		else if (system.isChargedown())
			return "SHUTTING DOWN";

		return null;
	}
}
