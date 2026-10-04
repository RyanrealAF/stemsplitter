package com.example.model

enum class ProcessingStage(val stepNumber: Int, val label: String) {
    IDLE(0, "Ready"),
    INPUT_VALIDATED(1, "Input Validated"),
    PREPARING(2, "Inspecting & Preparing Audio"),
    UPLOADING(3, "Uploading to Engine"),
    SEPARATING_HTDEMUCS(4, "Neural HTDemucs 6s Separation"),
    TRANSCRIBING_BASIC_PITCH(5, "Basic Pitch Polyphonic Transcription"),
    SAVING_STEMS(6, "Saving Stems & MIDI Output"),
    COMPLETED(7, "Separation Complete"),
    ERROR(-1, "Processing Error")
}
