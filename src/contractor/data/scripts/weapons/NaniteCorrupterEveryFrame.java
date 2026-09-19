package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.statuseffects.NaniteCorruption;
import contractor.data.scripts.statuseffects.StatusEffect;
import contractor.data.scripts.statuseffects.StatusEffectUtils;
import contractor.data.scripts.util.ContractorStaticVars;
import contractor.data.scripts.util.ContractorUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static contractor.data.scripts.campaign.items.DrgBiometalItemPlugin.*;

public class NaniteCorrupterEveryFrame implements EveryFrameWeaponEffectPluginWithAdvanceAfter {
	private final LinkedList<Vector2f> renderPoints = new LinkedList<>();
	private final LinkedList<Float> renderAngles = new LinkedList<>();
	private final Vector2f end = new Vector2f();
	private final Vector2f localTarget = new Vector2f();
	private boolean active = false, failed = false, flip = false;
	private DamagingProjectileAPI proj = null;
	private ShipAPI target = null;
	private float time = 0f, retractTime = 3f, lockedTime = 0f;
	private float projSpeed = 0f;
	private float initialFacing;
	public static final Map<ShipAPI.HullSize, Float> SIZE_MAP = new HashMap<>(); static {
		SIZE_MAP.put(ShipAPI.HullSize.FIGHTER, 0.01f);
		SIZE_MAP.put(ShipAPI.HullSize.FRIGATE, 1.1f);
		SIZE_MAP.put(ShipAPI.HullSize.DESTROYER, 1f);
		SIZE_MAP.put(ShipAPI.HullSize.CRUISER, 0.9f);
		SIZE_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.8f);
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (proj != null) {
			if (!proj.isFading()) {
				end.setX(proj.getLocation().getX());
				end.setY(proj.getLocation().getY());
			} else if (proj.isFading() && !active && !failed) {
				failed = true;
				retractTime = 3f;
				proj = null;
			}
		}
	}

	@Override
	public void advanceAfter(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (proj != null && !proj.isFading()) {
			end.setX(proj.getLocation().getX());
			end.setY(proj.getLocation().getY());
		} else if (proj != null && proj.isFading() && !active && !failed) {
			failed = true;
			retractTime = 3f;
			proj = null;
		}

		if ((active || failed || proj != null) && !VectorUtils.isZeroVector(end)) {
			MagicTrailPlugin trailPlugin = MagicTrailPlugin.getPlugin();
			if (trailPlugin != null) {
				float id = MagicTrailPlugin.getUniqueID();
				float id2 = MagicTrailPlugin.getUniqueID();
				Vector2f start = weapon.getFirePoint(0);

				if (failed) {
					Vector2f temp = ContractorUtils.getSegmentPoint(start, end, 1f - (amount * 0.33f));
					if (temp != null)
						end.set(temp.getX(), temp.getY());
				} else if (active) {
					Vector2f temp = new Vector2f();
					temp = VectorUtils.rotate(localTarget, target.getFacing() - initialFacing, temp);
					temp = Vector2f.add(temp, target.getLocation(), temp);
					end.set(temp.getX(), temp.getY());
				}

				float wepRange = weapon.getRange() * weapon.getRange();
				float dist = MathUtils.getDistanceSquared(start, end) / (wepRange);

				if (active) {
					prepRenderActive(start, amount, dist);
					doActiveActions(weapon, start, dist, amount, engine);
				} else if (failed)
					prepRenderFailed(start, amount);
				else if (proj != null)
					prepRenderTravel(start, weapon, amount);

				ShipAPI ship = weapon.getShip();

				if (active && (lockedTime > 20f || ship.getFluxTracker().isVenting() || ship.getFluxTracker().isOverloaded() || ship.isPhased() || weapon.isDisabled() || target.isPhased() || dist > 1.05f)) {
					active = false;
					lockedTime = 0f;
					failed = true;
					target = null;
				} else if (failed && retractTime <= 0f) {
					failed = false;
					proj = null;
					retractTime = 3f;
					end.set(0f, 0f);
					renderPoints.clear();
					renderAngles.clear();
				}

				while (!renderPoints.isEmpty() && !renderAngles.isEmpty()) {
					Vector2f point = renderPoints.poll();
					float angle = renderAngles.poll();
					float opacity;

					if (failed)
						opacity = Math.min(0.8f, retractTime / 3f);
					else
						opacity = 0.8f;

					SpriteAPI sprite = Global.getSettings().getSprite("fx", "base_trail_aura");
					SpriteAPI sprite2 = Global.getSettings().getSprite("fx", "base_trail_zap");
					Color color = Global.getSettings().getFactionSpec(ContractorStaticVars.CONTRACTOR_NANITE_ID).getColor();
					Color color2 = new Color(160, 190, 140, 255);

					MagicTrailPlugin.addTrailMemberSimple(ship, id, sprite, point, 0f, angle, 12f, 12f, color,
							opacity, 0f, amount, 0f, false);
					MagicTrailPlugin.addTrailMemberAdvanced(ship, id2, sprite2, point, 0f, 0f, angle, 0f, 0f, 18f, 18f, color2, color2,
							opacity * 0.4f, 0f, amount, 0f, true, 256f, -750f, -1f, null, null, null, 1f);
				}
			}
		}
	}

	private void doActiveActions(WeaponAPI weapon, Vector2f start, float dist, float amount, CombatEngineAPI engine) {
		weapon.setRemainingCooldownTo(weapon.getCooldown());
		weapon.getShip().getFluxTracker().setCurrFlux(weapon.getShip().getFluxTracker().getCurrFlux() + 200f * amount);
		ContractorUtils.applyForce(target, weapon.getLocation(), dist * 10f);
		float sizeScale = SIZE_MAP.get(target.getHullSize());
		float armorScale = Math.min(1.4f, 1000f / Math.min(2000f, target.getArmorGrid().getArmorRating()));
		armorScale = armorScale * armorScale;

		if (target.getVariant().hasHullMod(DRG_BIOMETAL_HULLMOD_ID) || target.getVariant().hasHullMod(DRG_REPAIR_NANITES_HULLMOD_ID) || target.getVariant().hasHullMod(DRG_NANITE_FORGES_HULLMOD_ID)) {
			NaniteCorruption plugin = (NaniteCorruption) StatusEffectUtils.addOrMaintainEffect(target, StatusEffect.StatusType.NANITE_CORRUPTION, amount * 2.5f, 1f);
			plugin.updateData(amount * 1.2f * sizeScale * armorScale);
		} else {
			NaniteCorruption plugin = (NaniteCorruption) StatusEffectUtils.addOrMaintainEffect(target, StatusEffect.StatusType.NANITE_CORRUPTION, amount * 2f, 1f);
			plugin.updateData(amount * 0.6f * sizeScale * armorScale);
		}

		Global.getSoundPlayer().playLoop("nanite_corrupter_loop", this, 1f, 1f, end, target.getVelocity());

		if (Math.random() > 0.93f) {
			EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
			params.segmentLengthMult = 10f;
			params.zigZagReductionFactor = 0.15f;
			params.fadeOutDist = 50f;
			params.minFadeOutMult = 10f;
			params.flickerRateMult = 0.3f;

			float distance = Misc.getDistance(start, end);
			float fraction = Math.min(0.33f, 300f / distance);
			params.brightSpotFullFraction = fraction;
			params.brightSpotFadeFraction = fraction;

			float arcSpeed = RiftLightningEffect.RIFT_LIGHTNING_SPEED;
			params.movementDurOverride = Math.max(0.05f, distance / arcSpeed);

			Color color = RiftLightningEffect.RIFT_LIGHTNING_COLOR;
			ShipAPI ship = weapon.getShip();
			EmpArcEntityAPI arc = engine.spawnEmpArcVisual(start, ship, end, target, 80f, Global.getSettings().getFactionSpec(ContractorStaticVars.CONTRACTOR_NANITE_ID).getColor(), color, params);
			arc.setCoreWidthOverride(40f);

			arc.setRenderGlowAtStart(false);
			arc.setFadedOutAtStart(true);
			arc.setSingleFlickerMode(true);

			Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1f, 0.9f, end, new Vector2f());
		}
	}

	private void prepRenderActive(Vector2f start, float amount, float dist) {
		if (dist > 1f)
			dist = 1f;

		float rotAngle;
		if (flip)
			rotAngle = (float) (35f * Math.sin(dist * MathUtils.FPI)) * -1;
		else
			rotAngle = (float) (35f * Math.sin(dist * MathUtils.FPI));

		Vector2f p1 = ContractorUtils.getSegmentPoint(start, end, 0.33f);
		Vector2f p2 = ContractorUtils.getSegmentPoint(start, end, 0.66f);
		p1 = VectorUtils.rotateAroundPivot(p1, start, Misc.normalizeAngle(-rotAngle));
		p2 = VectorUtils.rotateAroundPivot(p2, start, Misc.normalizeAngle(rotAngle));

		int j = (int) (44 * dist);
		if (j < 10)
			j = 10;
		for (int i = 0; i <= j; i++) {
			float ratio = (float) i / j;
			float angle;
			Vector2f point = ContractorUtils.fastCubicBezier(start, p1, p2, end, ratio);
			angle = Misc.getAngleInDegrees(ContractorUtils.fastCubicBezierDerivative(start, p1, p2, end, ratio));
			renderPoints.offer(point);
			renderAngles.offer(angle);
		}
		lockedTime += amount;
	}

	private void prepRenderFailed(Vector2f start, float amount) {
		float rotAngle;
		if (flip)
			rotAngle = (float) (35f * Math.sin((retractTime / 3f) * MathUtils.FPI)) * -1;
		else
			rotAngle = (float) (35f * Math.sin((retractTime / 3f) * MathUtils.FPI));

		Vector2f p1 = ContractorUtils.getSegmentPoint(start, end, 0.33f);
		Vector2f p2 = ContractorUtils.getSegmentPoint(start, end, 0.66f);
		p1 = VectorUtils.rotateAroundPivot(p1, start, Misc.normalizeAngle(-rotAngle));
		p2 = VectorUtils.rotateAroundPivot(p2, start, Misc.normalizeAngle(rotAngle));

		int j = (int) (44 * (retractTime / 3f));
		if (j < 10)
			j = 10;
		for (int i = 0; i <= j; i++) {
			float ratio = (float) i / j;
			float angle;
			Vector2f point = ContractorUtils.fastCubicBezier(start, p1, p2, end, ratio);
			angle = Misc.getAngleInDegrees(ContractorUtils.fastCubicBezierDerivative(start, p1, p2, end, ratio));
			renderPoints.offer(point);
			renderAngles.offer(angle);
		}
		retractTime -= amount;
	}

	private void prepRenderTravel(Vector2f start, WeaponAPI weapon, float amount) {
		float rotAngle;
		if (flip)
			rotAngle = (float) (35f * Math.sin(time * MathUtils.FPI)) * -1;
		else
			rotAngle = (float) (35f * Math.sin(time * MathUtils.FPI));

		Vector2f p1 = ContractorUtils.getSegmentPoint(start, end, 0.33f);
		Vector2f p2 = ContractorUtils.getSegmentPoint(start, end, 0.66f);
		p1 = VectorUtils.rotateAroundPivot(p1, start, Misc.normalizeAngle(-rotAngle));
		p2 = VectorUtils.rotateAroundPivot(p2, start, Misc.normalizeAngle(rotAngle));

		int j = (int) (44 * time);
		if (j < 10)
			j = 10;
		for (int i = 0; i <= j; i++) {
			float ratio = (float) i / j;
			float angle;
			Vector2f point = ContractorUtils.fastCubicBezier(start, p1, p2, end, ratio);
			angle = Misc.getAngleInDegrees(ContractorUtils.fastCubicBezierDerivative(start, p1, p2, end, ratio));
			renderPoints.offer(point);
			renderAngles.offer(angle);
		}
		time += amount * (projSpeed / weapon.getRange());
		if (time > 1f)
			time = 1f;
	}

	public void notifyFired(DamagingProjectileAPI projectile) {
		this.proj = projectile;
		this.projSpeed = projectile.getMoveSpeed();
		this.time = 0f;
		flip = Math.random() > 0.5f;
	}

	public void notifyHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit) {
		if (!shieldHit && !projectile.isFading() && target instanceof ShipAPI s) {
			active = true;
			time = 0f;

			if (s.isStationModule() && s.getParentStation().isShipWithModules()) {
				this.target = s.getParentStation();
			} else
				this.target = s;
			Vector2f hitInShipCoords = new Vector2f(Vector2f.sub(point, target.getLocation(), null));
			this.localTarget.set(hitInShipCoords.getX(), hitInShipCoords.getY());
			this.initialFacing = this.target.getFacing();

			NaniteCorruption plugin = (NaniteCorruption) StatusEffectUtils.addOrMaintainEffect(this.target, StatusEffect.StatusType.NANITE_CORRUPTION, 1f, 1f);
			plugin.updateData(projectile.getWeapon().getShip(), end, projectile.getWeapon().getShip().getOwner());
		} else {
			failed = true;
			retractTime = 3f;
		}

		this.end.setX(point.getX());
		this.end.setY(point.getY());

		this.proj = null;
	}
}
