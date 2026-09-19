package contractor.data.scripts.statuseffects;

import com.fs.starfarer.api.combat.ShipAPI;
import contractor.data.scripts.statuseffects.StatusEffect.StatusType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

import static contractor.data.scripts.util.ContractorStaticVars.STATUS_EFFECTS_ENABLED;

public class StatusEffectUtils {
	/**
	 * @param target Ship to affect.
	 * @param type Type of effect to work with.
	 * @param effectLevel Amount to add to effect length, in seconds.
	 * @param initial Amount to start effect length with in seconds, if applicable.
	 * @return StatusEffect plugin, newly created or already existing, may be null if no plugin created.
	 */
	public static StatusEffect addOrMaintainEffect(@NotNull ShipAPI target, StatusType type, float effectLevel, float initial) {
		return addOrMaintainEffect(target, type, effectLevel, initial, false, null);
	}

	/**
	 * @param target Ship to affect.
	 * @param effectLevel Amount to add to effect length, in seconds.
	 * @param initial Amount to start effect length with in seconds, if applicable.
	 * @param classOvrd .class representative for OTHER type StatusEffects, must inherit from or implement StatusEffect.
	 * @return StatusEffect plugin, newly created or already existing, may be null if no plugin created.
	 */
	public static StatusEffect addOrMaintainEffect(@NotNull ShipAPI target, float effectLevel, float initial, Class<?> classOvrd) {
		return addOrMaintainEffect(target, StatusType.OTHER, effectLevel, initial, false, classOvrd);
	}

	/**
	 * @param target Ship to affect.
	 * @param type Type of effect to work with.
	 * @param effectLevel Amount to add to effect length, in seconds.
	 * @param initial Amount to start effect length with in seconds, if applicable.
	 * @param ignore Whether to ignore the Immune effect if the target has it. Defaults to False.
	 * @return StatusEffect plugin, newly created or already existing, may be null if no plugin created.
	 */
	public static StatusEffect addOrMaintainEffect(@NotNull ShipAPI target, StatusType type, float effectLevel, float initial, boolean ignore, @Nullable Class<?> classOvrd) {
		if (!STATUS_EFFECTS_ENABLED || target == null)
			return null;
		if (!ignore && target.hasListenerOfClass(Immune.class))
			return null;

		StatusEffect plugin = null;

		switch (type) {
			case CRYOGENIC -> {
				if (target.hasListenerOfClass(Cryogenic.class)) {
					plugin = target.getListeners(Cryogenic.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Cryogenic(target, initial));
			}
			case ELECTRIC -> {
				if (target.hasListenerOfClass(Electric.class)) {
					plugin = target.getListeners(Electric.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Electric(target, initial));
			}
			case ENTROPIC -> {
				if (target.hasListenerOfClass(Entropic.class)) {
					plugin = target.getListeners(Entropic.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Entropic(target, initial));
			}
			case IMMUNE -> {
				if (target.hasListenerOfClass(Immune.class)) {
					plugin = target.getListeners(Immune.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Immune(target, initial, false));
			}
			case JAMMED -> {
				if (target.hasListenerOfClass(Jammed.class)) {
					plugin = target.getListeners(Jammed.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Jammed(target, initial));
			}
			case NANITE_CORRUPTION -> {
				if (target.hasListenerOfClass(NaniteCorruption.class)) {
					plugin = target.getListeners(NaniteCorruption.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new NaniteCorruption(target, initial));
			}
			case RADIATION -> {
				if (target.hasListenerOfClass(Radiation.class)) {
					plugin = target.getListeners(Radiation.class).get(0);
					plugin.addEffectLevel(effectLevel);
				} else
					target.addListener(plugin = new Radiation(target, initial));
			}
			case THERMAL -> { //todo: implement

			}
			case OTHER -> {
				if (true) return null; //disabled because Starsector doesn't allow reflections

				if (classOvrd == null)
					return null;

				if (target.hasListenerOfClass(classOvrd)) {
					plugin = (StatusEffect) target.getListeners(classOvrd).get(0);
					plugin.addEffectLevel(effectLevel);
				} else {
					if (StatusEffect.class.isAssignableFrom(classOvrd)) {
						try {
							Class<?>[] parameterType = {ShipAPI.class, float.class};
							Object[] args = {target, initial};
							plugin = (StatusEffect) classOvrd.getDeclaredConstructor(parameterType).newInstance(args);

							target.addListener(plugin);
						} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
								 NoSuchMethodException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		return plugin;
	}

	/**
	 * @param target Ship to affect.
	 * @param type Type of StatusEffect to remove.
	 */
	public static void clearEffect(@NotNull ShipAPI target, StatusType type) {
		clearEffect(target, type, null);
	}

	/**
	 * @param target Ship to affect.
	 * @param classOvrd class representative for OTHER type StatusEffects, must inherit from or implement StatusEffect.
	 */
	public static void clearEffect(@NotNull ShipAPI target, @Nullable Class<?> classOvrd) {
		clearEffect(target, StatusType.OTHER, classOvrd);
	}

	/**
	 * @param target Ship to affect.
	 * @param type Type of StatusEffect to remove.
	 * @param classOvrd class representative for OTHER type StatusEffects, must inherit from or implement StatusEffect.
	 */
	public static void clearEffect(@NotNull ShipAPI target, StatusType type, Class<?> classOvrd) {
		switch (type) {
			case CRYOGENIC -> {
				for (Cryogenic effect : target.getListeners(Cryogenic.class))
					target.removeListener(effect);
			}
			case ELECTRIC -> {
				for (Electric effect : target.getListeners(Electric.class))
					target.removeListener(effect);
			}
			case ENTROPIC -> {
				for (Entropic effect : target.getListeners(Entropic.class))
					target.removeListener(effect);
			}
			case IMMUNE -> {
				for (Immune effect : target.getListeners(Immune.class))
					target.removeListener(effect);
			}
			case JAMMED -> {
				for (Jammed effect : target.getListeners(Jammed.class))
					target.removeListener(effect);
			}
			case RADIATION -> {
				for (Radiation effect : target.getListeners(Radiation.class))
					target.removeListener(effect);
			}
			case THERMAL -> { //todo: implement
			}
			case OTHER -> { //todo: test
				if (classOvrd == null)
					return;

				if (StatusEffect.class.isAssignableFrom(classOvrd)) {
					StatusEffect cast = StatusEffect.class.cast(classOvrd);
					for (StatusEffect effect : target.getListeners(cast.getClass()))
						target.removeListener(effect);
				}
			}
		}
	}

	public static void clearAllEffects(@NotNull ShipAPI target) {
		for (StatusEffect effect : target.getListeners(StatusEffect.class))
			target.removeListener(effect);
	}
}
