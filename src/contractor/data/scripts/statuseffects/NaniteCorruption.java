package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.plugins.NaniteRevivePlugin;
import contractor.data.scripts.util.ContractorStaticVars;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static contractor.data.hullmods.NaniteHullmod.SHIP_BEING_REVIVED;
import static contractor.data.hullmods.NaniteHullmod.SPAWN_TIME;
import static contractor.data.scripts.campaign.items.DrgBiometalItemPlugin.*;

public class NaniteCorruption extends BaseStatusEffect {
	private static final String STATS_ID = "nanite_corruption_id";
	private final boolean hasNaniteHM, isFighter, validTarget;
	private ShipAPI source = null;
	private int newOwner;
	private int[] center;
	private float accumulation = 0f, corruptTime = 1f;
	private boolean corrupted = false, needRemove = true;
	private static final DamagingExplosionSpec SPEC = new DamagingExplosionSpec(0.1f, 8f, 8f, 0f, 0f, CollisionClass.HITS_SHIPS_ONLY_FF, CollisionClass.NONE, 6f,
			12f, 0.2f, 6, RiftLightningEffect.RIFT_LIGHTNING_COLOR, Global.getSettings().getFactionSpec(ContractorStaticVars.CONTRACTOR_NANITE_ID).getColor()); static {
		SPEC.setMinEMPDamage(30f);
		SPEC.setMaxEMPDamage(30f);
		SPEC.setUseDetailedExplosion(false);
	}

	public NaniteCorruption(@NotNull ShipAPI target, float initial) {
		super(target, initial);
		this.id = target.getId() + StatusType.NANITE_CORRUPTION;
		isFighter = target.isFighter();
		hasNaniteHM = target.getVariant().hasHullMod(DRG_BIOMETAL_HULLMOD_ID) || target.getVariant().hasHullMod(DRG_REPAIR_NANITES_HULLMOD_ID) || target.getVariant().hasHullMod(DRG_NANITE_FORGES_HULLMOD_ID);

		boolean test = true;
		for (String check : ContractorStaticVars.NANITE_PROTECTED_TARGETS)
			if (target.getHullSpec().getBaseHullId().equals(check))
				test = false;
		validTarget = !target.hasTag(ContractorStaticVars.CONTRACTOR_NO_NANITE_REGEN) && target != Global.getCombatEngine().getPlayerShip() && !target.isStation() && test;

		if (target.getOwner() == 0 && !isFighter)
			if (validTarget) {
				Global.getCombatEngine().getCombatUI().addMessage(0, target, Misc.getPositiveHighlightColor(), target.getName() + " is being corrupted by hostiles!");
				Global.getSoundPlayer().playUISound("cr_allied_malfunction", 1.15f, 0.85f);
			} else {
				Global.getCombatEngine().getCombatUI().addMessage(0, target, Misc.getPositiveHighlightColor(), target.getName() + " is being disabled by hostiles!");
				Global.getSoundPlayer().playUISound("cr_allied_warning", 1.15f, 0.85f);
			}
	}

	public void updateData(ShipAPI source, Vector2f point, int o) {
		this.source = source;
		this.center = target.getArmorGrid().getCellAtLocation(point);
		this.newOwner = o;
	}

	public void updateData(float amount) {
		accumulation += amount;
	}

	@Override
	public void advance(float amount) {
		CombatEngineAPI engine = Global.getCombatEngine();
		if (!target.isAlive()) {
			corruptTime += amount;
			if (target.isHulk() && !(target.isStationModule() || target.isStation()) && !target.hasTag(SHIP_BEING_REVIVED) && !isFighter && accumulation > 22f)
				engine.addPlugin(new NaniteRevivePlugin(target, SPAWN_TIME));
			if (corruptTime > SPAWN_TIME * 0.95f) {
				remove(false);
				return;
			}
		}

		float accumRatio = (accumulation / 30f);
		renderFX(accumRatio);

		int severity = Math.round(accumulation / 9);
		if (target.getOwner() != newOwner)
			armorDecay(severity);

		manageStats();

		if (accumulation >= 30f && !corrupted) {
			if (validTarget) {
				if (target.getOriginalOwner() == 0 && !isFighter) {
					engine.getCombatUI().addMessage(0, target, Misc.getNegativeHighlightColor(), target.getName() + " is no longer under your control!");
					Global.getSoundPlayer().playUISound("cr_allied_critical", 1.2f, 0.8f);
				}
				corrupted = true;
				target.setOwner(newOwner);
				if (target.isShipWithModules())
					for (ShipAPI child : target.getChildModulesCopy())
						child.setOwner(newOwner);
				effectLevel += 15f;
				target.getFluxTracker().beginOverloadWithTotalBaseDuration(5f);
			} else {
				target.getFluxTracker().beginOverloadWithTotalBaseDuration(5f);
			}
			accumulation -= 30f;
		}

		if (corrupted) {
			for (ShipAPI check : engine.getShips()) {
				if (check.isFighter())
					continue;
				if (check.getOwner() != newOwner) {
					engine.setCombatNotOverForAtLeast(4f);
					break;
				}
			}

			target.setRetreating(false, false);
			if (target.getNumFighterBays() > 0)
				for (FighterLaunchBayAPI bay : target.getLaunchBaysCopy())
					bay.setCurrRate(0f);
			corruptTime += amount;
			effectLevel -= amount * 0.5f;
			if (effectLevel < 0f) {
				effectLevel = 0f;
				if (!hasNaniteHM)
					accumulation -= amount;
			}
		} else {
			effectLevel -= amount;
			if (effectLevel < 0f) {
				effectLevel = 0f;
				accumulation -= amount * 1.25f;
			} else
				accumulation -= amount * 0.5f;
		}

		if (effectLevel > 0f)
			accumulation += amount * Math.min(3f, Math.max(0.4f, effectLevel / Math.max(1f, accumulation)));

		if (accumulation < 0f)
			accumulation = 0f;
		else if (accumulation > 60f)
			accumulation = 60f;

		if (effectLevel <= 0f && accumulation <= 0f && !isFighter) {
			MutableShipStatsAPI stats = target.getMutableStats();
			stats.getFluxDissipation().unmodify(STATS_ID);
			stats.getMaxSpeed().unmodify(STATS_ID);
			stats.getCombatWeaponRepairTimeMult().unmodify(STATS_ID);
			stats.getCombatEngineRepairTimeMult().unmodify(STATS_ID);

			if (corrupted) {
				if (target.getOriginalOwner() == 0 && target.getOwner() == 1) {
					engine.getCombatUI().addMessage(0, target, Misc.getPositiveHighlightColor(), target.getName() + " has returned to your control");
					Global.getSoundPlayer().playUISound("cr_allied_warning", 1.3f, 0.85f);
				}
				target.setOwner(target.getOriginalOwner());
				if (target.isShipWithModules())
					for (ShipAPI child : target.getChildModulesCopy())
						child.setOwner(child.getOriginalOwner());
			}
			remove(false);
		}
	}


	private void renderFX(float accumRatio) {
		float ratioScaled = accumRatio;
		if (corrupted)
			ratioScaled *= 0.6f;
		if (Math.random() > 0.97f - ratioScaled * 0.1f) {
			EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
			params.segmentLengthMult = 12f;
			params.zigZagReductionFactor = 0.25f;
			params.fadeOutDist = 30f;
			params.minFadeOutMult = 12f;
			params.flickerRateMult = 0.3f;

			Vector2f start;
			if (Math.random() > 0.4f && center != null)
				start = MathUtils.getRandomPointInCircle(target.getArmorGrid().getLocation(center[0], center[1]), 12f);
			else
				start = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius() * 0.65f);
			Vector2f end = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius() * 0.65f);
			float distance = Misc.getDistance(start, end);
			float fraction = Math.min(0.18f, 300f / distance);
			float arcSpeed = RiftLightningEffect.RIFT_LIGHTNING_SPEED;

			params.brightSpotFullFraction = fraction;
			params.brightSpotFadeFraction = fraction;
			params.movementDurOverride = Math.max(0.05f, distance / arcSpeed);

			Color color = RiftLightningEffect.RIFT_LIGHTNING_COLOR;
			float thickness = Math.max(12f, accumRatio * 48f);

			EmpArcEntityAPI arc = Global.getCombatEngine().spawnEmpArcVisual(start, target, end, target, thickness, Global.getSettings().getFactionSpec(ContractorStaticVars.CONTRACTOR_NANITE_ID).getColor(), color, params);
			arc.setCoreWidthOverride(thickness * 0.5f);

			arc.setRenderGlowAtStart(false);
			arc.setFadedOutAtStart(true);
			arc.setSingleFlickerMode(true);

			if (!corrupted)
				Global.getCombatEngine().spawnDamagingExplosion(SPEC, source, end, false); //not strictly FX but the end point is already here

			Global.getSoundPlayer().playSound("mote_attractor_impact_damage", 1.1f, 0.7f, end, new Vector2f());

			if (corrupted) {
				DwellerShroud shroud = DwellerShroud.getShroudFor(target);
				if (corruptTime < 10f) {
					if (shroud == null) {
						DwellerShroud.DwellerShroudParams fxparams = DwellerShroud.createBaselineParams(target);
						fxparams.memberRespawnRate = 60f;
						fxparams.color = Global.getSettings().getFactionSpec(ContractorStaticVars.CONTRACTOR_NANITE_ID).getColor();
						fxparams.negativeParticleSizeMult = 0.7f;
						fxparams.alphaMult = 0.9f;
						new DwellerShroud(target, fxparams);
					} else
						shroud.getParams().alphaMult = 10f / (corruptTime * 10f);
				} else if (needRemove) {
					shroud.setForceDespawn(true);
					needRemove = false;
				}
			}
		}

		if (target == Global.getCombatEngine().getPlayerShip()) {
			if (Global.getSettings().isDevMode()) {
				Global.getCombatEngine().maintainStatusForPlayerShip("nanite_status_key", "icon_tactical_reality_disruptor", "Nanite Corruption",
						"Systems Compromised: " + Math.round(effectLevel) + " " + Math.round(accumulation), true);
			} else {
				String data;
				if (accumulation > 23f)
					data = "Critical";
				else if (accumulation > 10f)
					data = "Severe";
				else
					data = "Moderate";
				Global.getCombatEngine().maintainStatusForPlayerShip("nanite_status_key", "icon_tactical_reality_disruptor", "Nanite Corruption",
						"Systems Compromised: " + data, true);
			}
		}
	}

	private void armorDecay(int severity) {
		ArmorGridAPI grid = target.getArmorGrid();
		if (center != null) {
			float[][] gridArray = grid.getGrid();

			switch (severity) {
				case 0 -> {
					float armorVal = grid.getArmorValue(center[0], center[1]);
					armorVal *= 0.9999f;
					grid.setArmorValue(center[0], center[1], armorVal);
				}
				case 1 -> {
					for (int i = -1; i < 2; i++) {
						for (int j = -1; j < 2; j++) {
							int x = i + center[0];
							int y = j + center[1];

							if (0 <= x && x < gridArray.length) {
								if (0 <= y && y < gridArray[x].length) {
									float armorVal = grid.getArmorValue(x, y);
									armorVal *= 0.9998f;
									grid.setArmorValue(x, y, armorVal);
								}
							}
						}
					}
				}
				case 2 -> {
					for (int i = -2; i < 3; i++) {
						for (int j = -2; j < 3; j++) {
							int x = i + center[0];
							int y = j + center[1];

							if (0 <= x && x < gridArray.length) {
								if (0 <= y && y < gridArray[x].length) {
									float armorVal = grid.getArmorValue(x, y);
									armorVal *= 0.9997f;
									grid.setArmorValue(x, y, armorVal);
								}
							}
						}
					}
				}
				case 3 -> {
					for (int i = -3; i < 4; i++) {
						for (int j = -3; j < 4; j++) {
							int x = i + center[0];
							int y = j + center[1];

							if (0 <= x && x < gridArray.length) {
								if (0 <= y && y < gridArray[x].length) {
									float armorVal = grid.getArmorValue(x, y);
									armorVal *= 0.9996f;
									grid.setArmorValue(x, y, armorVal);
								}
							}
						}
					}
				}
			}
		}
	}

	private void manageStats() {
		MutableShipStatsAPI stats = target.getMutableStats();

		if (accumulation > 20f) {
			stats.getFluxDissipation().modifyMult(STATS_ID, 0.89f);
			stats.getMaxSpeed().modifyMult(STATS_ID, 0.89f);
			stats.getCombatWeaponRepairTimeMult().modifyMult(STATS_ID, 0.89f);
			stats.getCombatEngineRepairTimeMult().modifyMult(STATS_ID, 0.89f);
		} else if (accumulation > 10f) {
			stats.getFluxDissipation().modifyMult(STATS_ID, 0.78f);
			stats.getMaxSpeed().modifyMult(STATS_ID, 0.78f);
			stats.getCombatWeaponRepairTimeMult().modifyMult(STATS_ID, 0.78f);
			stats.getCombatEngineRepairTimeMult().modifyMult(STATS_ID, 0.78f);
		} else {
			stats.getFluxDissipation().modifyMult(STATS_ID, 0.67f);
			stats.getMaxSpeed().modifyMult(STATS_ID, 0.67f);
			stats.getCombatWeaponRepairTimeMult().modifyMult(STATS_ID, 0.67f);
			stats.getCombatEngineRepairTimeMult().modifyMult(STATS_ID, 0.67f);
		}
	}
}
