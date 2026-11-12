#!/bin/bash
set -e

echo "=========================================================="
echo " Taegeuk Coach: Suite Local de Verificación (Fase F1)     "
echo "=========================================================="

echo "-> 1. Ensamblado Debug..."
./gradlew assembleDebug

echo "-> 2. Pruebas Unitarias JVM (:app y :analysis)..."
./gradlew testDebugUnitTest

echo "-> 3. Análisis Estático Detekt..."
./gradlew detekt

echo "-> 4. Verificación de Formato y Estilo ktlint..."
./gradlew ktlintCheck

echo "=========================================================="
echo " ¡TODAS LAS VERIFICACIONES COMPLETADAS EN VERDE!          "
echo "=========================================================="

