package contractor.data.scripts.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation;
import com.fs.starfarer.api.impl.campaign.procgen.Constellation.ConstellationType;
import com.fs.starfarer.api.impl.campaign.procgen.NameGenData;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarAge;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.Nullable;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.util.Misc.RAD_PER_DEG;
import static contractor.data.scripts.campaign.fleets.DisposableNaniteFleetManager.*;
import static contractor.data.scripts.util.ContractorStaticVars.*;

public class ContractorUtils {
	public static int countActiveCommandMissiles(ShipAPI ship) {
		int count = 0;
		if (ship.isFighter())
			return count;
		for (WeaponAPI wep : ship.getAllWeapons()) {
			if (wep.getSpec().hasTag("sepcmdmis")) {
				CommandMissileInterface plugin = (CommandMissileInterface) wep.getEffectPlugin();
				count += plugin.getActive();
			}
		}
		return count;
	}

	public static int getCommandMissileMax(ShipAPI ship) {
		int max = COMMAND_MISSILE_BASE.get(ship.getHullSize());
		if (ship.getVariant().hasHullMod("eccm"))
			max += ECCM_BONUS;
		if (ship.getVariant().hasHullMod("contractor_missilecommand"))
			max += MC_BONUS;
		else if (ship.isStation() || ship.isStationModule())
			max += MC_BONUS;

		return max;
	}

	public static boolean isNaniteHull(MutableShipStatsAPI stats) {
		if (stats == null) return false;
		FleetMemberAPI member = stats.getFleetMember();
		if (member == null) return false;
		return member.getHullSpec().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID) ||
				member.getVariant().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID);
	}

	public static boolean isNaniteHull(ShipAPI ship) {
		if (ship == null) return false;
		return ship.getHullSpec().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID) ||
				ship.getVariant().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID);
	}

	/**
	 * Computes how close the given system is to the Nanite 'home system' cluster.
	 * @param system System to check.
	 * @return Integer representing which distance 'block' the system is in. Will return a negative value if said system is not eligible for spawning random fleets.
	 */
	public static int computeNaniteDistFactor(StarSystemAPI system) {
		StarSystemAPI homeSys = (StarSystemAPI) Global.getSector().getPersistentData().get(CONTRACTOR_NANITE_HOME_SYSTEM);
		float dist = Misc.getDistanceLY(system.getLocation(), homeSys.getLocation());

		if (dist <= DIST_0)
			return 0;
		else if (dist <= DIST_1)
			return 1;
		else if (dist <= DIST_2)
			return 2;
		else if (dist > DIST_2 + 5f)
			return -1;
		else return 3;
	}

	/**
	 * Returns the largest player market in the system with the largest player population. If multiple markets fulfill this, returns the first found. Returns null if player has no markets.
	 */
	public static MarketAPI getLargestPlayerMarket() {
		MarketAPI largestMarket = null;
		for (final StarSystemAPI system : Misc.getSystemsWithPlayerColonies(false)) {
			List<MarketAPI> playerMarkets = Misc.getMarketsInLocation(system, Factions.PLAYER);
			for (MarketAPI market : playerMarkets) {
				if (largestMarket == null)
					largestMarket = market;
				else if (market.getSize() > largestMarket.getSize())
					largestMarket = market;
				else if (market.getSize() == largestMarket.getSize()
						&& getPlayerMarketSizeSum(market.getStarSystem()) > getPlayerMarketSizeSum(largestMarket.getStarSystem()))
					largestMarket = market;
			}
		}
		return largestMarket;
	}

	/**
	 * Sums all player market's sizes in the system. Returns 0 if none found.
	 * @param system System to test.
	 * @return Sum of all player market's sizes in the system.
	 */
	public static int getPlayerMarketSizeSum(StarSystemAPI system) {
		int size = 0;
		for (MarketAPI market : Misc.getMarketsInLocation(system, Factions.PLAYER)) {
			size += market.getSize();
		}
		return size;
	}

	public static Constellation quickConstellation(ConstellationType type, StarAge age, String name) {
		Constellation constellation = new Constellation(type, age);
		NameGenData constellationNameData = new NameGenData(name, name);
		constellationNameData.addTag(NameGenData.TAG_CONSTELLATION);
		constellation.setNamePick(new ProcgenUsedNames.NamePick(constellationNameData, name, name)); //prevents crash, something with name and intel system
		return constellation;
	}

	/**
	 * Applies force to a combat entity.
	 * @param entity Entity to apply force to.
	 * @param aim Location toward which to apply force, in absolute engine coordinates.
	 * @param force Force to apply, is scaled by 100.
	 */
	//derived from Lazylib
	public static void applyForce(CombatEntityAPI entity, Vector2f aim, float force) {
		applyForce(entity, aim, force, 1f);
	}

	/**
	 * Applies force to a combat entity.
	 * @param entity Entity to apply force to.
	 * @param aim Location toward which to apply force, in absolute engine coordinates.
	 * @param force Force to apply, is scaled by 100.
	 * @param massScale Amount to scale targets mass by. (lower mass is moved faster)
	 */
	//derived from Lazylib
	public static void applyForce(CombatEntityAPI entity, Vector2f aim, float force, float massScale) {
		Vector2f copy = new Vector2f(aim);
		force *= 100f;
		float mass = Math.max(1f, entity.getMass() * massScale);
		float velChange = Math.min(1000f, force / mass);
		Vector2f.sub(copy, entity.getLocation(), copy);
		Vector2f dir = new Vector2f();
		copy.normalise(dir);
		dir.scale(velChange);
		Vector2f.add(dir, entity.getVelocity(), entity.getVelocity());
	}

	/**
	 *  Does not return fighters
	 */
	//derived from Lazylib
	public static List<ShipAPI> getAllyShipsWithinRange(Vector2f location, float range, int owner) {
		List<ShipAPI> ships = new ArrayList<>();

		for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
			if (tmp.isShuttlePod() || tmp.isFighter()) {
				continue;
			}

			if (tmp.getOwner() == owner && MathUtils.isWithinRange(tmp, location, range)) {
				ships.add(tmp);
			}
		}
		return ships;
	}

	public static float getAverageArmor(ArmorGridAPI grid) {
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

	public static Vector2f fastUnitVector(float degrees) {
		Vector2f result = new Vector2f();
		float radians = degrees * RAD_PER_DEG;
		result.x = (float) FastTrig.cos(radians);
		result.y = (float) FastTrig.sin(radians);
		return result;
	}

	/**
	 * Computes a point on a cubic Bezier curve.
	 * @param p0 Start point.
	 * @param p1 p0's control point.
	 * @param p2 p3's control point.
	 * @param p3 End point.
	 * @param ratio Ratio or timescale for computed point, must be 0 <= ratio <= 1
	 * @return A new Vector2f representing the computed point.
	 */
	public static Vector2f fastCubicBezier(Vector2f p0, Vector2f p1, Vector2f p2, Vector2f p3, float ratio) {
		Vector2f goal = new Vector2f();

		if (ratio <= 0f) {
			goal.setX(p0.getX());
			goal.setY(p0.getY());
			return goal;
		} else if (ratio >= 1f) {
			goal.setX(p3.getX());
			goal.setY(p3.getY());
			return goal;
		}

		float rInverse = 1f - ratio;
		float x1, x2, x3, x4, y1, y2, y3, y4;

		x1 = rInverse * rInverse * rInverse * p0.getX();
		y1 = rInverse * rInverse * rInverse * p0.getY();

		x2 = 3f * rInverse * rInverse * ratio * p1.getX();
		y2 = 3f * rInverse * rInverse * ratio * p1.getY();

		x3 = 3f * rInverse * ratio * ratio * p2.getX();
		y3 = 3f * rInverse * ratio * ratio * p2.getY();

		x4 = ratio * ratio * ratio * p3.getX();
		y4 = ratio * ratio * ratio * p3.getY();


		goal.setX(x1 + x2 + x3 + x4);
		goal.setY(y1 + y2 + y3 + y4);

		return goal;
	}

	/**
	 * Computes the derivative for a point on a cubic Bezier curve.
	 * @param p0 Start point.
	 * @param p1 p0's control point.
	 * @param p2 p3's control point.
	 * @param p3 End point.
	 * @param ratio Ratio or timescale for computed point, must be 0 <= ratio <= 1
	 * @return A new Vector2f representing the computed point.
	 */
	public static Vector2f fastCubicBezierDerivative(Vector2f p0, Vector2f p1, Vector2f p2, Vector2f p3, float ratio) {
		Vector2f goal = new Vector2f();

		if (ratio < 0f || ratio > 1f)
			return goal;

		float rInverse = 1f - ratio;
		float x1, x2, x3, y1, y2, y3;

		x1 = 3 * (rInverse * rInverse) * (p1.getX() - p0.getX());
		y1 = 3 * (rInverse * rInverse) * (p1.getY() - p0.getY());

		x2 = 6 * rInverse * ratio * (p2.getX() - p1.getX());
		y2 = 6 * rInverse * ratio * (p2.getY() - p1.getY());

		x3 = 3 * (ratio * ratio) * (p3.getX() - p2.getX());
		y3 = 3 * (ratio * ratio) * (p3.getY() - p2.getY());

		goal.set(x1 + x2 + x3, y1 + y2 + y3);

		return goal;
	}

	/**
	 * Clamps a float to its third decimal for display purposes.
	 */
	public static float clampFloatToThird(float toClamp) {
		double angleClamp = toClamp;
		angleClamp *= 1000;
		angleClamp = Math.floor(angleClamp);
		angleClamp = angleClamp / 1000;
		return (float) angleClamp;
	}

	/**
	 * Creates a List of two Vector2Fs, the start point, and the end point scaled to the map size.
	 * @param startPoint Point to cast from.
	 * @param angle angle to build ray.
	 * @return List of two Vector2F's, may return null if CombatEngine is not initialized.
	 */
	public static List<Vector2f> buildRayEnds(Vector2f startPoint, float angle) {
		List<Vector2f> ray = new ArrayList<>(2);
		float h;
		float w;
		if (Global.getCombatEngine() != null) {
			CombatEngineAPI engine = Global.getCombatEngine();
			h = engine.getMapHeight() / 2;
			w = engine.getMapWidth() / 2;
		} else
			return null;

		float x = startPoint.x;
		float y = startPoint.y;
		float dx = (float) Math.cos(angle);
		float dy = (float) Math.sin(angle);
		while (x < w && x > -w && y < h && y > -h) {
			x += dx;
			y += dy;
		}
		x -= dx;
		y -= dy;
		ray.add(new Vector2f(startPoint));
		ray.add(new Vector2f(x, y));

		return ray;
	}

	/**
	 * Creates a List of Vector2Fs, between the start point and the end point scaled to the map size.
	 * @param startPoint Point to cast from.
	 * @param angle Angle to build ray.
	 * @param density Number of points along the ray, should be a reasonably high number given the potential size of the ray. Minimum 100.
	 * @return List of Vector2Fs, may return null if CombatEngine is not initialized.
	 */
	public static List<Vector2f> buildRay(Vector2f startPoint, float angle, int density) {
		List<Vector2f> ray = new ArrayList<>();
		int densityCheck = density;
		if (densityCheck < 100)
			densityCheck = 100;
		float densityScale = 1f / densityCheck;
		float h;
		float w;
		if (Global.getCombatEngine() != null) {
			CombatEngineAPI engine = Global.getCombatEngine();
			h = engine.getMapHeight() / 2;
			w = engine.getMapWidth() / 2;
		} else
			return null;

		float x = startPoint.x;
		float y = startPoint.y;
		float dx = (float) Math.cos(angle);
		float dy = (float) Math.sin(angle);
		while (x < w && x > -w && y < h && y > -h) {
			x += dx;
			y += dy;
		}
		x -= dx;
		y -= dy;
		Vector2f endPoint = new Vector2f(x, y);
		for (float i = 0f; i <= 1f; i += densityScale) {
			x = (startPoint.x - (i * startPoint.x)) + (i * endPoint.x);
			y = (startPoint.y - (i * startPoint.y)) + (i * endPoint.y);
			Vector2f toAdd = new Vector2f(x, y);
			ray.add(toAdd);
		}

		return ray;
	}

	/**
	 * gets a Vector2f point on the line between the specified ends.
	 * @param startPoint start of segment.
	 * @param endPoint end of segment.
	 * @param relPoint relative position of point to get, with 0 being the start, and 1 being the end.
	 * @return A new Vector2f with the specified relative position, returns null if 0 <= relPoint <= 1 is not true.
	 */
	public static Vector2f getSegmentPoint(Vector2f startPoint, Vector2f endPoint, float relPoint) {
		if (relPoint < 0f || relPoint > 1f || startPoint == null || endPoint == null)
			return null;
		float x = (startPoint.x - (relPoint * startPoint.x)) + (relPoint * endPoint.x);
		float y = (startPoint.y - (relPoint * startPoint.y)) + (relPoint * endPoint.y);

		return new Vector2f(x, y);
	}

	/**
	 * Creates a list of Vector2f points between specified end points.
	 * @param startPoint start of segment.
	 * @param endPoint end of segment.
	 * @param density number of points -1 (less the start point) on segment, minimum 2.
	 * @return List of Vector2f containing all points.
	 */
	public static List<Vector2f> buildRaySegment(Vector2f startPoint, Vector2f endPoint, int density) {
		List<Vector2f> segment = new ArrayList<>(density + 2);
		int densityCheck = density;
		if (densityCheck < 2)
			densityCheck = 2;
		float densityScale = 1f / densityCheck;
		for (float i = 0f; i <= 1f; i += densityScale) {
			float x = (startPoint.x - (i * startPoint.x)) + (i * endPoint.x);
			float y = (startPoint.y - (i * startPoint.y)) + (i * endPoint.y);
			Vector2f toAdd = new Vector2f(x, y);
			segment.add(toAdd);
		}
		return segment;
	}

	/**
	 * Tests for ships along a line between two specified points at 1/100 intervals and returns the first ship found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @return The first ship found during ray cast, may be null if none found.
	 */
	public static @Nullable ShipAPI simpleRayCastShip(Vector2f startPoint, Vector2f endPoint) {
		return simpleRayCastShip(startPoint, endPoint, 4f, 100, true, null);
	}

	/**
	 * Tests for ships along a line between two specified points at 1/100 intervals and returns the first ship found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param exclude Ship to ignore during ray cast, typically includes the firing ship when used for weapons. May be null.
	 * @return The first ship found during ray cast, may be null if none found.
	 */
	public static @Nullable ShipAPI simpleRayCastShip(Vector2f startPoint, Vector2f endPoint, ShipAPI exclude) {
		List<ShipAPI> list = new ArrayList<>();
		list.add(exclude);
		return simpleRayCastShip(startPoint, endPoint, 4f, 100, true, list);
	}

	/**
	 * Tests for ships along a line between two specified points at 1/100 intervals and returns the first ship found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param exclude Array of ships to ignore when ray casting, typically includes the firing ship when used for weapons. May be null.
	 * @return The first ship found during ray cast, may be null if none found.
	 */
	public static @Nullable ShipAPI simpleRayCastShip(Vector2f startPoint, Vector2f endPoint, @Nullable List<ShipAPI> exclude) {
		return simpleRayCastShip(startPoint, endPoint, 4f, 100, true, exclude);
	}

	/**
	 * Tests for ships along a line between two specified points and returns the first ship found.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param width Additional distance from / collision radius of the ray points to test for ships.
	 * @param density How dense the line of points is to check along path, minimum 2.
	 * @param ignoreFighter Whether to ignore fighters during check.
	 * @param exclude List of ships to ignore when ray casting, typically includes the firing ship when used for weapons. May be null.
	 * @return The first ship found during ray cast, may be null if none found.
	 */
	public static @Nullable ShipAPI simpleRayCastShip(Vector2f startPoint, Vector2f endPoint, float width, int density, boolean ignoreFighter, @Nullable List<ShipAPI> exclude) {
		ShipAPI eval = null;
		int densityCheck = density;
		if (densityCheck < 2)
			densityCheck = 2;
		final float densityScale = 1f / densityCheck;
		final List<ShipAPI> allShips = Global.getCombatEngine().getShips();

		for (float i = 0f; i <= 1f; i += densityScale) {
			float x = (startPoint.x - (i * startPoint.x)) + (i * endPoint.x);
			float y = (startPoint.y - (i * startPoint.y)) + (i * endPoint.y);
			Vector2f toTest = new Vector2f(x, y);
			List<ShipAPI> potential = new ArrayList<>();


			for (ShipAPI test : allShips) {
				if ((ignoreFighter && test.isFighter()))
					continue;

				if (MathUtils.isWithinRange(test.getLocation(), toTest, width + (test.getCollisionRadius() * 0.5f))) {
					potential.add(test);
					break;
				}
			}

			if (potential.isEmpty())
				continue;

			if (exclude != null) {
				for (ShipAPI ex : exclude) {
					if (potential.get(0) != ex) {
						break;
					}
				}
			}
			eval = potential.get(0);
		}
		return eval;
	}

	/**
	 * Tests for ships along a line between two specified points and returns all ships found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @return The list of ships found during ray cast, may be empty if none found.
	 */
	public static List<ShipAPI> rayCastListShip(Vector2f startPoint, Vector2f endPoint) {
		return rayCastListShip(startPoint, endPoint, 4f, 100, true, null);
	}

	/**
	 * Tests for ships along a line between two specified points and returns all ships found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param exclude Ship to ignore during ray cast, typically the firing ship when used for weapons. May be null.
	 * @return The list of ships found during ray cast, may be empty if none found.
	 */
	public static List<ShipAPI> rayCastListShip(Vector2f startPoint, Vector2f endPoint, ShipAPI exclude) {
		List<ShipAPI> list = new ArrayList<>();
		list.add(exclude);
		return rayCastListShip(startPoint, endPoint, 4f, 100, true, list);
	}

	/**
	 * Tests for ships along a line between two specified points and returns all ships found, ignores fighters.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param exclude Array of ships to ignore when ray casting, typically includes the firing ship when used for weapons. May be null.
	 * @return The list of ships found during ray cast, may be empty if none found.
	 */
	public static List<ShipAPI> rayCastListShip(Vector2f startPoint, Vector2f endPoint, @Nullable List<ShipAPI> exclude) {
		return rayCastListShip(startPoint, endPoint, 4f, 100, true, exclude);
	}

	/**
	 * Tests for ships along a line between two specified points and returns all ships found.
	 * @param startPoint Start of ray.
	 * @param endPoint End of ray.
	 * @param width Additional distance from ray points to test for ships.
	 * @param density How dense the line of points is to check along path, minimum 2.
	 * @param ignoreFighter Whether to ignore fighters during check.
	 * @param exclude List of ships to ignore when ray casting, typically includes the firing ship when used for weapons. May be null.
	 * @return The list of ships found during ray cast, may be empty if none found.
	 */
	public static List<ShipAPI> rayCastListShip(Vector2f startPoint, Vector2f endPoint, float width, int density, boolean ignoreFighter, @Nullable List<ShipAPI> exclude) {
		Set<ShipAPI> eval = new HashSet<>();
		int densityCheck = density;
		if (densityCheck < 2)
			densityCheck = 2;
		final float densityScale = 1f / densityCheck;
		final List<ShipAPI> allShips = Global.getCombatEngine().getShips();

		for (float i = 0f; i <= 1f; i += densityScale) {
			float x = (startPoint.x - (i * startPoint.x)) + (i * endPoint.x);
			float y = (startPoint.y - (i * startPoint.y)) + (i * endPoint.y);
			Vector2f toTest = new Vector2f(x, y);
			List<ShipAPI> potential = new ArrayList<>();

			for (ShipAPI test : allShips) {
				if ((ignoreFighter && test.isFighter()))
					continue;

				if (MathUtils.isWithinRange(test.getLocation(), toTest, width + (test.getCollisionRadius() * 0.5f)))
					potential.add(test);
			}

			if (potential.isEmpty())
				continue;

			if (exclude != null && !exclude.isEmpty()) {
				for (ShipAPI potentialCheck : potential) {
					boolean valid = true;
					for (ShipAPI ex : exclude) {
						if (ex == potentialCheck) {
							valid = false;
							break;
						}
					}
					if (valid)
						eval.add(potentialCheck);
				}
			} else
				eval.addAll(potential);
		}

		return new ArrayList<ShipAPI>(eval);
	}
}

