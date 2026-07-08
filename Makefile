# Gimme Vetting Solution - Project Build Orchestration
# Targets: build, test, clean, backend, frontend, backend-test, frontend-test, check-tools

SHELL := /bin/bash

.PHONY: build test clean backend frontend backend-test frontend-test check-tools

# Default target
build: backend frontend

# Run all tests across all subprojects
test: backend-test frontend-test

# Remove all build artifacts
clean:
	@echo "Cleaning frontend dist..."
	rm -rf 4-frontend/dist
	@echo "Cleaning backend target directories..."
	find 5-backend -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
	@echo "Clean complete."

# Build backend only
backend: check-tools
	@echo "Building backend..."
	cd 5-backend && mvn clean package -DskipTests
	@echo "Backend build complete."

# Build frontend only
frontend: check-tools
	@echo "Building frontend..."
	cd 4-frontend && npm run build
	@echo "Frontend build complete."

# Test backend only
backend-test: check-tools
	@echo "Testing backend..."
	cd 5-backend && mvn test
	@echo "Backend test complete."

# Test frontend only
frontend-test: check-tools
	@echo "Testing frontend..."
	cd 4-frontend && npm test
	@echo "Frontend test complete."

# Verify required tools are installed
check-tools:
	@echo "Checking required tools..."
	@MISSING=; \
	for tool in mvn node npm; do \
		if ! command -v $$tool >/dev/null 2>&1; then \
			MISSING="$$MISSING $$tool"; \
		fi; \
	done; \
	if [ -n "$$MISSING" ]; then \
		echo "ERROR: Missing tools:$$MISSING"; \
		echo "Please install the missing tools before building."; \
		echo ""; \
		echo "Suggested installation commands:"; \
		case "$$MISSING" in \
			*"mvn"*) echo "  Maven: apt-get install maven  (Debian/Ubuntu) or brew install maven (macOS)";; \
		esac; \
		case "$$MISSING" in \
			*"node"*) echo "  Node.js: apt-get install nodejs  (Debian/Ubuntu) or brew install node (macOS)";; \
		esac; \
		case "$$MISSING" in \
			*"npm"*) echo "  npm: apt-get install npm  (Debian/Ubuntu) or brew install npm (macOS)";; \
		esac; \
		exit 1; \
	fi
	@echo "All required tools are installed."
