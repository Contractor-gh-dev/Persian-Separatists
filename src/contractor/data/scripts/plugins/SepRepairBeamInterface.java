package contractor.data.scripts.plugins;

import com.fs.starfarer.api.combat.ShipAPI;

public interface SepRepairBeamInterface {

	float getMinimumAverageArmor(ShipAPI ship);

	void updateMinimumAverageArmor(ShipAPI ship);
}
