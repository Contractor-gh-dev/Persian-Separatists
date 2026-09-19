package contractor.data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.combat.ShipSystemAPI.SystemState;
import contractor.data.hullmods.EntangledMod;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;

import static org.lazywizard.lazylib.VectorUtils.getAngle;

public class EntanglementStats extends BaseShipSystemScript {
	public static final float BASE_RANGE = 2250f;
	private static final Color JITTER_COLOR = new Color(145, 2, 105, 45);
	private static final Color JITTER_UNDER_COLOR = new Color(175, 60, 120, 150);
	private static final Color ARC_COLOR = new Color(175, 60, 160, 255);
	private static final String DAUGHTERKEY = "EntangledKey_";
	private ShipAPI daughterShip = null;
	private Vector2f targetPos = null;


	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		CombatEngineAPI engine = Global.getCombatEngine();
		float arcAngle = 0f;
		String PARENTKEY = "";
		if (stats.getEntity() instanceof ShipAPI ship) {
			PARENTKEY = ship.getId();
		} else
			return;

		float range = stats.getSystemRangeBonus().computeEffective(BASE_RANGE);
		float jitterRangeBonus = 0f;
		float maxRangeBonus = 7f;
		if (state == State.IN) {
			jitterRangeBonus = effectLevel * maxRangeBonus;
		} else if (state == State.ACTIVE) {
			jitterRangeBonus = maxRangeBonus;
		} else if (state == State.OUT) {
			jitterRangeBonus = Math.max(7f, (maxRangeBonus / (effectLevel + 0.01f)));
		}

		ship.setJitter(this, JITTER_COLOR, effectLevel, 3, 0, 0f + jitterRangeBonus);
		ship.setJitterUnder(this, JITTER_UNDER_COLOR, effectLevel, 25, 0f, 7f + jitterRangeBonus);

		if (targetPos == null && state == State.IN) {
			targetPos = new Vector2f(ship.getMouseTarget());
			float distance = MathUtils.getDistanceSquared(ship.getLocation(), targetPos);
			float check = range - 250f;
			if (distance > check * check) {
				targetPos = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(ship.getLocation(), targetPos));
				targetPos.scale(check);
				Vector2f.add(targetPos, ship.getLocation(), targetPos);
			}
			if (engine.getPlayerShip() != null) {
				if (!ship.getId().equals(engine.getPlayerShip().getId())) {
					Vector2f midPoint = new Vector2f(ship.getLocation());
					Vector2f.add(midPoint, targetPos, midPoint);
					midPoint.scale(0.5f);
					targetPos = midPoint;
				}
			}

			Vector2f zeroed = new Vector2f(0f, 0f);
			Vector2f spriteSize = new Vector2f(Global.getSettings().getSprite("systems", ship.getHullSpec().getHullId() + "_sprite").getWidth(),
					Global.getSettings().getSprite("systems", ship.getHullSpec().getHullId() + "_sprite").getHeight());
			arcAngle = (getAngle(ship.getLocation(), targetPos) - 90f);

			MagicRender.battlespace(Global.getSettings().getSprite("systems", ship.getHullSpec().getHullId() + "_sprite"), targetPos, zeroed, spriteSize, zeroed, arcAngle, 0, JITTER_UNDER_COLOR, true,
					12f, 4f, 0.5f, 0.5f, 0.1f, 0.1f, 2f, 0.1f, CombatEngineLayers.BELOW_INDICATORS_LAYER);
			engine.spawnExplosion(targetPos, zeroed, JITTER_UNDER_COLOR, 256f, 2.5f);
		}

		if (state == State.ACTIVE && daughterShip == null)
			daughterShip = spawnShip(ship, targetPos);

		if (state == State.ACTIVE && daughterShip != null) {
			if (engine.getCustomData().get(DAUGHTERKEY + daughterShip.getId()) != null) {
				EntangledMod.EntangledModData data = (EntangledMod.EntangledModData) engine.getCustomData().get(DAUGHTERKEY + daughterShip.getId());
				data.parentID = PARENTKEY;
			}
			float distance = MathUtils.getDistanceSquared(ship.getLocation(), daughterShip.getLocation());
			if (distance >= range * range) {
				fireEMP(engine, daughterShip, ship);
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			}
			if (!daughterShip.isAlive())
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
			if (daughterShip.isRetreating())
				ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
		}
	}

	public void unapply(MutableShipStatsAPI stats, String id) {
		if (daughterShip != null) {
			CombatEngineAPI engine = Global.getCombatEngine();
			Global.getSoundPlayer().playSound("mine_explosion", 1f, 1f, daughterShip.getLocation(), new Vector2f(0f, 0f));
			daughterShip.setHitpoints(0);
			engine.getFleetManager(daughterShip.getOriginalOwner()).removeDeployed(daughterShip, false);
			engine.getFleetManager(daughterShip.getOriginalOwner()).removeFromReserves(daughterShip.getFleetMember());
			engine.getCustomData().remove(DAUGHTERKEY + daughterShip.getId());
			daughterShip = null;
		}
	}

	protected ShipAPI spawnShip(ShipAPI parent, Vector2f targetPosition) {
		float arcAngle = getAngle(parent.getLocation(), targetPosition);
		ShipAPI daughterShip = null;
		CombatFleetManagerAPI fleetManager = Global.getCombatEngine().getFleetManager(parent.getOriginalOwner());
		ArmorGridAPI parentArmor = parent.getArmorGrid();
		ShipVariantAPI daughterVar = Global.getSettings().getVariant(parent.getHullSpec().getHullId() + "_clone");
		daughterVar.clear();

		for (String mod : parent.getVariant().getHullMods())
			daughterVar.addMod(mod);

		for (WeaponAPI w : parent.getAllWeapons())
			daughterVar.addWeapon(w.getSlot().getId(), w.getId());
		daughterVar.addWeapon("ENT01", "drg_entangled_spark"); //required slot id on hull

		daughterVar.autoGenerateWeaponGroups();

		daughterVar.setNumFluxCapacitors(parent.getVariant().getNumFluxCapacitors());
		daughterVar.setNumFluxVents(parent.getVariant().getNumFluxVents());


		boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
		fleetManager.setSuppressDeploymentMessages(true);

		daughterShip = fleetManager.spawnShipOrWing(daughterVar.getHullVariantId(), targetPosition, arcAngle);

		fleetManager.setSuppressDeploymentMessages(wasSuppressed);


		daughterShip.setCurrentCR(parent.getCurrentCR());
		daughterShip.setName(parent.getName() + " clone");
		daughterShip.setHitpoints(parent.getHitpoints());
		daughterShip.setTimeDeployed(parent.getTimeDeployedForCRReduction());
		daughterShip.getCaptain().setPersonality(Personalities.RECKLESS);

		ArmorGridAPI daughterArmor = daughterShip.getArmorGrid();
		for (int i = 0; i < parentArmor.getGrid().length; i++)
			for (int j = 0; j < parentArmor.getGrid()[i].length; j++)
				daughterArmor.setArmorValue(i, j, parentArmor.getArmorValue(i, j));
		daughterShip.syncWithArmorGridState();
		daughterShip.syncWeaponDecalsWithArmorDamage();

		Global.getCombatEngine().spawnExplosion(targetPos, new Vector2f(0f, 0f), JITTER_UNDER_COLOR, 384f, 1f);
		targetPos = null;
		return daughterShip;
	}

	private void fireEMP(CombatEngineAPI engine, ShipAPI from, ShipAPI to) {
		engine.spawnEmpArcPierceShields(from, from.getLocation(), from, to, DamageType.ENERGY, 100f, 1250f, 3000f,
				"system_emp_emitter_impact", 75f, JITTER_UNDER_COLOR, ARC_COLOR);
	}

	public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
		if (system.isActive()) {
			if (Misc.getDistance(ship.getLocation(), daughterShip.getLocation()) > 2000f)
				return "RANGE WARNING";
			else
				return "ACTIVE";
		}

		Vector2f target = ship.getMouseTarget();

		if (system.getState() == SystemState.IDLE && (Misc.getDistance(ship.getLocation(), target)) > 2000f)
			return "OUT OF RANGE";

		return null;
	}

	public String getDaughter() {
		if (daughterShip == null)
			return "";
		return daughterShip.getId();
	}
}


