# Mob Stats Tweaks — NeoForge 1.21.1

Mod de servidor ligero para modificar estadísticas base de mobs vanilla a través de `config/mobstatstweaks.json`.

## Qué hace

- Modifica la vida máxima, daño de ataque, velocidad de movimiento, armadura, dureza de armadura, resistencia al retroceso y alcance de seguimiento.
- Aplica multiplicadores al aparecer o cargarse entidades.
- Evita multiplicar repetidamente las estadísticas: guarda los valores base originales en los datos persistentes de cada entidad.
- Permite reglas por ID de entidad que reemplazan los multiplicadores globales solo para las estadísticas especificadas.
- Permite recargar la configuración y actualizar/restaurar entidades ya cargadas con `/mobstatstweaks reload` (requiere nivel de permiso 2).
- Por defecto solo afecta entidades `Mob` del namespace `minecraft` y hostiles (`Enemy`). Una regla explícita de entidad permite incluir una entidad no hostil vanilla aunque `hostile_only` sea `true`.

## Instalar

1. Compila el proyecto con Java 21 y Gradle usando el MDK de NeoForge 1.21.1.
2. Copia `build/libs/mobstatstweaks-1.0.0.jar` a la carpeta `mods` del servidor NeoForge 1.21.1.
3. Arranca el servidor una vez. Se generará `config/mobstatstweaks.json`.
4. Edita el JSON y ejecuta `/mobstatstweaks reload`. También puedes reiniciar el servidor.

## Configuración

Todos los multiplicadores deben ser mayores que 0 y como máximo 100. `1.0` significa sin cambios. En `entities`, los valores para un mob reemplazan el global solo en las estadísticas que estén presentes; las otras siguen usando el valor global.

- `enabled`: activa o desactiva el sistema. Al desactivarlo y recargar, intenta restaurar los valores originales de entidades cargadas.
- `vanilla_only`: `true` limita el sistema a IDs `minecraft:*`; `false` también permite mobs de otros mods.
- `hostile_only`: `true` aplica la regla global solo a mobs que implementan la interfaz de enemigo. Una regla explícita en `entities` también puede afectar a una entidad no hostil.
- `global`: multiplicadores generales.
- `entities`: personalización por ID registrado.

Ejemplo: zombi con 2× vida y 1.5× daño, esqueleto con 1.5× vida y más alcance:

```json
{
  "enabled": true,
  "vanilla_only": true,
  "hostile_only": true,
  "global": {
    "max_health": 1.5,
    "attack_damage": 1.25,
    "movement_speed": 1.1,
    "armor": 1.0,
    "armor_toughness": 1.0,
    "knockback_resistance": 1.0,
    "follow_range": 1.1
  },
  "entities": {
    "minecraft:zombie": {
      "max_health": 2.0,
      "attack_damage": 1.5
    },
    "minecraft:skeleton": {
      "max_health": 1.5,
      "follow_range": 1.35
    },
    "minecraft:creeper": {
      "max_health": 1.5,
      "follow_range": 1.25
    }
  }
}
```

Nota: el daño explosivo del creeper no es el atributo `attack_damage`, así que esta primera versión no multiplica el daño de explosiones, proyectiles ni daño ambiental. Tampoco cambia IA, equipo ni encantamientos. Solo ajusta atributos que la entidad tenga registrados. Las entidades que ya estaban cargadas se actualizan con el comando de recarga; al guardar/reabrir el mundo sus datos originales se conservan para evitar acumulación.

## Compilar

Requisitos: Java 21 y Gradle compatible con el plugin NeoGradle. Abre la carpeta como proyecto Gradle en IntelliJ IDEA o ejecuta `gradle build`. El JAR se genera en `build/libs/`.

Este archivo es el código fuente del mod; no se incluye un JAR compilado porque este entorno no tiene acceso a los repositorios Maven/Gradle necesarios para resolver NeoForge y ejecutar una compilación real.
