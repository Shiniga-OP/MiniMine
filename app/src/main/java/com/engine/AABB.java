package com.engine;

public class AABB {
    public final float minX, minY, minZ;
    public final float maxX, maxY, maxZ;
    private static final float EPSILON = 0.001f;

    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        // Garante valores válidos (min <= max)
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public AABB expand(float x, float y, float z) {
        return new AABB(
            minX - x, minY - y, minZ - z,
            maxX + x, maxY + y, maxZ + z
        );
    }

    public AABB offset(float dx, float dy, float dz) {
        return new AABB(
            minX + dx, minY + dy, minZ + dz,
            maxX + dx, maxY + dy, maxZ + dz
        );
    }

    public float clipXCollide(AABB other, float dx) {
        if (Math.abs(dx) < EPSILON) return dx;
        if (!intersectsYZ(other)) return dx;

        if (dx > 0) {
            float penetration = other.maxX - minX;
            if (penetration < dx) dx = penetration;
        } else {
            float penetration = other.minX - maxX;
            if (penetration > dx) dx = penetration;
        }

        return dx;
    }

    public float clipYCollide(AABB other, float dy) {
        if (Math.abs(dy) < EPSILON) return dy;
        if (!intersectsXZ(other)) return dy;

        if (dy > 0) {
            float penetration = other.maxY - minY;
            if (penetration < dy) dy = penetration;
        } else {
            float penetration = other.minY - maxY;
            if (penetration > dy) dy = penetration;
        }

        return dy;
    }

    public float clipZCollide(AABB other, float dz) {
        if (Math.abs(dz) < EPSILON) return dz;
        if (!intersectsXY(other)) return dz;

        if (dz > 0) {
            float penetration = other.maxZ - minZ;
            if (penetration < dz) dz = penetration;
        } else {
            float penetration = other.minZ - maxZ;
            if (penetration > dz) dz = penetration;
        }

        return dz;
    }

    // Métodos auxiliares otimizados para verificação de interseção
    private boolean intersectsYZ(AABB other) {
        return other.maxY > minY && other.minY < maxY &&
			other.maxZ > minZ && other.minZ < maxZ;
    }

    private boolean intersectsXZ(AABB other) {
        return other.maxX > minX && other.minX < maxX &&
			other.maxZ > minZ && other.minZ < maxZ;
    }

    private boolean intersectsXY(AABB other) {
        return other.maxX > minX && other.minX < maxX &&
			other.maxY > minY && other.minY < maxY;
    }

    // Novo método para verificação de interseção em Y (usado no step-down)
    public boolean intersectsY(AABB other) {
        return other.maxY >= minY && other.minY <= maxY;
    }

    // Método para calcular a distância de penetração em Y
    public float getYPenetration(AABB other) {
        if (other.maxY > minY && other.minY < maxY) {
            return other.maxY - minY;
        }
        return 0;
    }
	
	public float calcularPenetracaoX(AABB other) {
		if (other.maxY <= minY || other.minY >= maxY) return 0;
		if (other.maxZ <= minZ || other.minZ >= maxZ) return 0;

		if (maxX > other.minX && maxX <= other.maxX) {
			return other.minX - maxX; // Colisão à direita
		}

		if (minX < other.maxX && minX >= other.minX) {
			return other.maxX - minX; // Colisão à esquerda
		}

		return 0;
	}

	public float calcularPenetracaoY(AABB other) {
		if (other.maxX <= minX || other.minX >= maxX) return 0;
		if (other.maxZ <= minZ || other.minZ >= maxZ) return 0;

		if (maxY > other.minY && maxY <= other.maxY) {
			return other.minY - maxY; // Colisão acima
		}

		if (minY < other.maxY && minY >= other.minY) {
			return other.maxY - minY; // Colisão abaixo
		}

		return 0;
	}

	public float calcularPenetracaoZ(AABB other) {
		if (other.maxX <= minX || other.minX >= maxX) return 0;
		if (other.maxY <= minY || other.minY >= maxY) return 0;

		if (maxZ > other.minZ && maxZ <= other.maxZ) {
			return other.minZ - maxZ; // Colisão à frente
		}

		if (minZ < other.maxZ && minZ >= other.minZ) {
			return other.maxZ - minZ; // Colisão atrás
		}

		return 0;
	}

	public boolean intersects(AABB other) {
		return minX < other.maxX &&
			maxX > other.minX &&
			minY < other.maxY &&
			maxY > other.minY &&
			minZ < other.maxZ &&
			maxZ > other.minZ;
	}
}
