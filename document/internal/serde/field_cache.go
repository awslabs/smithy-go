package serde

import (
	"strings"
	"sync"
)

var fieldCache fieldCacher

type fieldCacher struct {
	cache sync.Map
}

func (c *fieldCacher) Load(t interface{}) (*CachedFields, bool) {
	if v, ok := c.cache.Load(t); ok {
		return v.(*CachedFields), true
	}
	return nil, false
}

func (c *fieldCacher) LoadOrStore(t interface{}, fs *CachedFields) (*CachedFields, bool) {
	v, ok := c.cache.LoadOrStore(t, fs)
	return v.(*CachedFields), ok
}

type CachedFields struct {
	fields       []Field
	fieldsByName map[string]int
}

func (f *CachedFields) All() []Field {
	return f.fields
}

func (f *CachedFields) FieldByName(name string) (Field, bool) {
	if i, ok := f.fieldsByName[name]; ok {
		return f.fields[i], ok
	}
	for _, f := range f.fields {
		if strings.EqualFold(f.Name, name) {
			return f, true
		}
	}
	return Field{}, false
}
