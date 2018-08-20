#!/usr/bin/python

from in_toto.models.layout import Layout, Step
from in_toto.models.metadata import Metablock
from in_toto.util import generate_and_write_rsa_keypair, import_rsa_key_from_file

layout = Layout()
build = Step(name="build")
analyze = Step(name="analyze")
layout.steps.append(build)
layout.steps.append(analyze)
generate_and_write_rsa_keypair("root_key")
root_key = import_rsa_key_from_file("root_key")

metablock = Metablock(signed=layout)
metablock.sign(root_key)
metablock.dump("root.layout")
