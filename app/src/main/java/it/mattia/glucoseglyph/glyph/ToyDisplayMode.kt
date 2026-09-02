package it.mattia.glucoseglyph.glyph

/**
 * What the Glyph Toy currently shows in the value+arrow slot. Cycled by pressing the Glyph button
 * on the back of the phone or by shaking it (see GlucoseToyService); always starts back at GLUCOSE
 * whenever the toy is (re)bound, so the primary reading is what you see by default.
 */
enum class ToyDisplayMode {
    GLUCOSE, PUMP_BATTERY, RESERVOIR;

    fun next(): ToyDisplayMode = entries[(ordinal + 1) % entries.size]
}
