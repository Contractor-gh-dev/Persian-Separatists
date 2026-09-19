package contractor.data.scripts.plugins;

import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;

import java.util.HashMap;

public class SepRepairBeamManager implements AdvanceableListener, SepRepairBeamInterface {
	private final HashMap<ShipAPI, Float> managedShips = new HashMap<>();

	@Override
	public float getMinimumAverageArmor(ShipAPI ship) {
		if (managedShips.get(ship) != null)
			return managedShips.get(ship);
		return -1f;
	}

	@Override
	public void updateMinimumAverageArmor(ShipAPI ship) {
		if (managedShips.get(ship) == null) {
			float avgArmorFrac = pollArmor(ship.getArmorGrid());
			managedShips.put(ship, avgArmorFrac);
			return;
		}

		float prevArmorFrac = managedShips.get(ship);
		float avgArmorFrac = pollArmor(ship.getArmorGrid());

		if (avgArmorFrac < prevArmorFrac)
			managedShips.put(ship, avgArmorFrac);
	}

	private float pollArmor(ArmorGridAPI grid) {
		float armor = 0;
		int count = 0;
		float[][] armorGrid = grid.getGrid();
		for (int i = 0; i < armorGrid.length; i++)
			for (int j = 0; j < armorGrid[i].length; j++) {
				armor += grid.getArmorFraction(i, j);
				count++;
			}
		armor = armor / count;
		return armor;
	}

	public void advance(float amount) {
	}
}
