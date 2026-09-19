package contractor.data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import contractor.data.scripts.plugins.SepRepairBeamManager;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.Point;

import java.awt.*;

import static contractor.data.scripts.util.ContractorStaticVars.REPAIR_BEAM_RATIO;

public class SepRepairBeamEffect implements BeamEffectPlugin {
	private static final float REPAIR_RATE = 0.03f;
	private static final Color fringe = new Color(0, 155, 0);
	private static final EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams(); static {
		params.glowSizeMult = 0.5f;
		params.glowAlphaMult = 0.8f;
	}
	private float timer = 0f;

	@Override
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		ShipAPI source = beam.getSource();
		if (source == null || target == null)
			return;

		timer += amount;

		if (timer > 0.2f) {
			timer = 0f;
			if (target instanceof ShipAPI ship) {
				if (!ship.isAlive())
					return;

				if (!engine.getListenerManager().hasListenerOfClass(SepRepairBeamManager.class))
					engine.getListenerManager().addListener(new SepRepairBeamManager());

				SepRepairBeamManager plugin = engine.getListenerManager().getListeners(SepRepairBeamManager.class).get(0);

				plugin.updateMinimumAverageArmor(ship);
				float minArmor = plugin.getMinimumAverageArmor(ship) + REPAIR_BEAM_RATIO;
				if (minArmor > 1f)
					minArmor = 1f;
				Point point = DefenseUtils.getMostDamagedArmorCell(ship);

				if (point == null)
					return;

				ArmorGridAPI grid = ship.getArmorGrid();

				if (ship.getArmorGrid().getArmorFraction(point.getX(), point.getY()) >= minArmor)
					return;

				float maxArmor = grid.getMaxArmorInCell();
				float newArmor = grid.getArmorValue(point.getX(), point.getY()) + grid.getMaxArmorInCell() * REPAIR_RATE;
				if (newArmor > maxArmor)
					newArmor = maxArmor;

				grid.setArmorValue(point.getX(), point.getY(), newArmor);

				if (Math.random() > 0.5d)
					engine.spawnEmpArcPierceShields(source, beam.getRayEndPrevFrame(), target, target, DamageType.FRAGMENTATION, 0f, 1f, target.getCollisionRadius(),
							"sep_repair_arc", 5f, fringe, Color.white, params);
				else
					engine.spawnEmpArcPierceShields(source, beam.getRayEndPrevFrame(), target, target, DamageType.FRAGMENTATION, 0f, 1f, target.getCollisionRadius(),
							"sep_repair_arc_low", 5f, fringe, Color.white, params);
				//Global.getCombatEngine().maintainStatusForPlayerShip("repairbeam_dev", Global.getSettings().getSpriteName("systems", "contractor_commandmissile"), "Working", newArmor + "", false);
			}
		}
	}
}
