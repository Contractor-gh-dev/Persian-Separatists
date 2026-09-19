package contractor.data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.plugins.DrgGabrielBeamRender;
import contractor.data.scripts.util.ContractorUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion.createStandardRiftParams;
import static com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion.spawnStandardRift;
import static org.lazywizard.lazylib.VectorUtils.getAngle;

public class DrgGabrielBeamEffect implements BeamEffectPlugin {
	private boolean hasFired = false, addedPlugin = false;
	private final IntervalUtil interval = new IntervalUtil(0.25f, 0.35f);
	private CombatEntityAPI renderer = null;
	private static final java.awt.Color col1 = new java.awt.Color(255, 180, 215, 150);
	private static final java.awt.Color col2 = new java.awt.Color(255, 200, 225, 150);
	private static final Color exp_under_col = new Color(150, 0, 50, 150);
	public static final float PER_POINT_DAMAGE = 80f;
	private static final String ID = "drg_gabriel_effect";
	private static final EmpArcEntityAPI.EmpArcParams PARAMS1 = new EmpArcEntityAPI.EmpArcParams(); static {
		PARAMS1.zigZagReductionFactor = 2f;
		PARAMS1.segmentLengthMult = 24f;
		PARAMS1.minFadeOutMult = 1.5f;
		PARAMS1.flickerRateMult = 0.6f;
	}

	private static final EmpArcEntityAPI.EmpArcParams PARAMS2 = new EmpArcEntityAPI.EmpArcParams();

	static {
		PARAMS2.zigZagReductionFactor = 2f;
		PARAMS2.segmentLengthMult = 20f;
		PARAMS2.minFadeOutMult = 1.75f;
		PARAMS2.flickerRateMult = 0.8f;
		PARAMS2.glowSizeMult = 0.5f;
	}

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		ShipAPI source = beam.getSource();
		if (source == null)
			return;

		for (WeaponAPI wep : source.getAllWeapons()) {
			if (wep.isDecorative())
				if (wep.getId().equals("drg_seraphmain_deco"))
					wep.setForceFireOneFrame(true);
		}

		float beamBrightness = beam.getBrightness();

		if (!hasFired) {
			Vector2f startPoint = beam.getFrom();
			Vector2f endPoint = beam.getTo();
			if (beamBrightness > 0f) {
				source.getMutableStats().getTurnAcceleration().modifyMult(ID, 0.5f);
				source.getMutableStats().getMaxTurnRate().modifyMult(ID, 0.5f);

				if (!addedPlugin) {
					addedPlugin = true;

					NegativeExplosionVisual.NEParams p = createStandardRiftParams("riftcascade_minelayer", 25f);
					p.fadeIn = 5.9f;
					p.fadeOut = 0.2f;
					p.thickness = 15f;
					p.blackColor = Color.white;
					p.underglow = exp_under_col;
					p.color = Color.MAGENTA;

					CombatEntityAPI e = engine.addLayeredRenderingPlugin(new DrgGabrielBeamRender(p, beam.getWeapon()));

					float x = (startPoint.x - (0.98f * startPoint.x)) + (0.98f * endPoint.x);
					float y = (startPoint.y - (0.98f * startPoint.y)) + (0.98f * endPoint.y);
					Vector2f endPointShifted = new Vector2f(x, y);
					e.getLocation().set(endPointShifted);
					renderer = e;
				} else {
					renderer.getLocation().set(endPoint);
				}

				interval.advance(amount + (amount * beamBrightness * 3));
				if (interval.intervalElapsed()) {
					int i = 0;
					do {
						float rand = (float) Math.random();

						float x = (startPoint.x - (rand * startPoint.x)) + (rand * endPoint.x);
						float y = (startPoint.y - (rand * startPoint.y)) + (rand * endPoint.y);
						Vector2f targetPoint = new Vector2f(x, y);

						List<ShipAPI> potentialTarget = CombatUtils.getShipsWithinRange(targetPoint, 4f);

						if (!potentialTarget.isEmpty()) {
							if (potentialTarget.get(0) != source) {
								if (potentialTarget.get(0).getCollisionClass() != CollisionClass.NONE) {
									EmpArcEntityAPI arc = engine.spawnEmpArc(source, startPoint, source, potentialTarget.get(0), DamageType.ENERGY, 20f, 20f, 1150f, "pseudoparticle_jet_hit_heavy",
											6f + 26f * beamBrightness, col2, col1, PARAMS2);
									arc.setCoreWidthOverride(4f + 12f * beamBrightness);
									arc.setSingleFlickerMode(true);
									arc.setWarping(1f);
								}
							}
						} else {
							EmpArcEntityAPI arc = engine.spawnEmpArcVisual(startPoint, source, targetPoint, null, 6f + 26f * beamBrightness, col2, col1, PARAMS2);
							arc.setCoreWidthOverride(4f + 12f * beamBrightness);
							arc.setSingleFlickerMode(true);
							arc.setWarping(1f);
						}

						if (beamBrightness >= 0.35f) {
							Vector2f randPoint = Misc.getPointWithinRadius(endPoint, 192f);

							EmpArcEntityAPI arc = engine.spawnEmpArcVisual(endPoint, source, randPoint, null, 4f + 18f * beamBrightness, col2, col1, PARAMS2);
							arc.setCoreWidthOverride(4f + 12f * beamBrightness);
							arc.setSingleFlickerMode(true);
							arc.setWarping(1f);
						}

						i++;
					} while (i < Math.round(1 + beamBrightness * 3));
				}
			}
			if (beamBrightness >= 1f) {
				hasFired = true;

				source.getMutableStats().getTurnAcceleration().unmodify(ID);
				source.getMutableStats().getMaxTurnRate().unmodify(ID);

				final List<Vector2f> segment = ContractorUtils.buildRaySegment(startPoint, endPoint, 100);
				final HashMap<ShipAPI, Boolean> rememberedThreat = new HashMap<>(2);
				final List<ShipAPI> allShips = engine.getShips();

				for (Vector2f point : segment) {
					for (ShipAPI targetShip : allShips) {
						float range = targetShip.getCollisionRadius() * 0.8f;
						if (MathUtils.isWithinRange(targetShip.getLocation(), point, range)) {
							boolean skipDamage = false;

							if (targetShip == source)
								continue;
							if (rememberedThreat.get(targetShip) != null)
								skipDamage = true;
							else if (targetShip.getHullSpec().hasTag("dweller") && !targetShip.getHullSpec().hasTag("dweller_ejecta")) {
								rememberedThreat.putIfAbsent(targetShip, false);
								skipDamage = true;
							}
							if (rememberedThreat.get(targetShip) != null && rememberedThreat.get(targetShip) == false) {
								rememberedThreat.put(targetShip, true);
								rememberedThreat.put(spawnShip(targetShip, endPoint), true);
								Global.getSoundPlayer().playSound("system_tenebrous_expulsion_activate", 1f, 1f, targetShip.getLocation(), targetShip.getVelocity());
							}

							if (!skipDamage)
								engine.applyDamage(targetShip, point, PER_POINT_DAMAGE, DamageType.HIGH_EXPLOSIVE, PER_POINT_DAMAGE * 0.75f, true, true, source);
						}
					}
				}
				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(startPoint, source, endPoint, null, 128f, col2, col1, PARAMS1);
				arc.setCoreWidthOverride(96f);
				arc.setSingleFlickerMode(true);
				arc.setWarping(1.5f);

				DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(0.1f, 100f, 50f, PER_POINT_DAMAGE * 2.5f, PER_POINT_DAMAGE * 0.5f, CollisionClass.PROJECTILE_FF,
						CollisionClass.PROJECTILE_FIGHTER, 4f, 12f, 1f, 24, Color.lightGray, Color.black);
				DamagingProjectileAPI exp = engine.spawnDamagingExplosion(explosionSpec, source, endPoint, true);

				NegativeExplosionVisual.NEParams p = createStandardRiftParams("riftcascade_minelayer", 50f);
				p.thickness = 50f;
				p.numRiftsToSpawn = 1;
				p.blackColor = Color.white;
				p.underglow = exp_under_col;
				p.color = Color.MAGENTA;
				spawnStandardRift(exp, p);
				Global.getSoundPlayer().playSound("drg_seraphmain_explode", 1f, 0.9f, endPoint, new Vector2f(0f, 0f));
			}
		}
	}

	private ShipAPI spawnShip(ShipAPI parent, Vector2f targetPosition) {
		float arcAngle = getAngle(parent.getLocation(), targetPosition);
		//Vector2f randPos = MathUtils.getRandomPointInCircle(targetPosition, parent.getCollisionRadius() * 1.5f);
		ShipAPI daughterShip = null;
		CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(parent.getOriginalOwner());
		ArmorGridAPI parentArmor = parent.getArmorGrid();
		ShipVariantAPI daughterVar = parent.getVariant();

		boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
		fleetManager.setSuppressDeploymentMessages(true);

		daughterShip = fleetManager.spawnShipOrWing(daughterVar.getHullVariantId(), targetPosition, arcAngle);

		fleetManager.setSuppressDeploymentMessages(wasSuppressed);


		daughterShip.setCurrentCR(parent.getCurrentCR());
		daughterShip.setName(parent.getName());
		daughterShip.setHitpoints(parent.getHitpoints());
		daughterShip.setTimeDeployed(parent.getTimeDeployedForCRReduction());
		daughterShip.getCaptain().setPersonality(Personalities.RECKLESS);

		ArmorGridAPI daughterArmor = daughterShip.getArmorGrid();
		for (int i = 0; i < parentArmor.getGrid().length; i++)
			for (int j = 0; j < parentArmor.getGrid()[i].length; j++)
				daughterArmor.setArmorValue(i, j, parentArmor.getArmorValue(i, j));
		daughterShip.syncWithArmorGridState();
		daughterShip.syncWeaponDecalsWithArmorDamage();

		return daughterShip;
	}
}
