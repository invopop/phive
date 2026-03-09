//go:build mage

package main

import (
	"github.com/invopop/tasks"
)

// Release a new version based on the current branch and timestamp.
func Release() error {
	return tasks.Release()
}
