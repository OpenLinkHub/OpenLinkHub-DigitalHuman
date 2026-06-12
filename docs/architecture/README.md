# Architecture

Architecture notes, diagrams, contracts, and provider-boundary decisions for OpenLinkHub DigitalHuman.

The most important design rule is provider replaceability: ASR, LLM, TTS, RAG, and avatar rendering should be hidden behind stable project contracts so cloud services and local models can be swapped without changing the frontend protocol.
