# PapiflyFX Code Specification

This directory contains the design and specification documents for the **PapiflyFX Code**, a VS Code-inspired text editor component built for the PapiflyFX Docking framework.

## Documents

- [**spec.md**](spec.md): The core architectural specification and contracts.
- [**implementation.md**](implementation.md): The phased delivery plan, milestones, and validation strategy.
- [**PROGRESS.md**](PROGRESS.md): Current implementation status and completed milestones.

## Overview

The goal of this component is to provide a high-performance, programmable text editor that integrates seamlessly with the docking framework's pure-code philosophy and theming system.

## Target Module

This spec targets creation of a separate Maven module:
- `papiflyfx-docking-code`

Integration boundary:
- `papiflyfx-docking-code` depends on `papiflyfx-docking-docks`.
- `papiflyfx-docking-docks` does not depend on `papiflyfx-docking-code`.

## Prompts

> check this document spec/papiflyfx-docking-code/spec.md, validate and make suggestion if it's possible to make more 
> concise or if anything missed. add your souggestions to additions.md
