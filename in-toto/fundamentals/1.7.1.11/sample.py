#!/usr/bin/python

from in_toto.models.layout import Layout, Step
from in_toto.models.metadata import Metablock
from in_toto.util import generate_and_write_rsa_keypair, import_rsa_key_from_file

layout = Layout()
build = Step(name="build")
build.expected_materials.append(['ALLOW', 'src/*'])
build.expected_products.append(['CREATE', 'foo'])
build.expected_command = ['gcc', '-o foo', 'src/*']
analyze = Step(name="analyze")
analyze.expected_materials.append(
  ['MATCH', 'foo', 'WITH', 'PRODUCTS', 'FROM', 'build'])
analyze.expected_command = ['valgrind', './foo']
layout.steps.append(build)
layout.steps.append(analyze)
generate_and_write_rsa_keypair("root_key")
root_key = import_rsa_key_from_file("root_key")

metablock = Metablock(signed=layout)
metablock.sign(root_key)
metablock.dump("root.layout")
